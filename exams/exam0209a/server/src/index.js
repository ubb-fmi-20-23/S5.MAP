const Koa = require('koa');
const app = new Koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({server});
const Router = require('koa-router');
const cors = require('koa-cors');
const bodyparser = require('koa-bodyparser');

app.use(bodyparser());
app.use(cors());
app.use(async function (ctx, next) {
    const start = new Date();
    await next();
    const ms = new Date() - start;
    console.log(`${ctx.method} ${ctx.url} ${ctx.response.status} - ${ms}ms`);
});

const getRandomInt = (min, max) => {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min)) + min;
};

const bikeSampleNames = ['BigAir', 'StreetRocker', 'MountainRyder', 'SlopeStyler'];
const bikeTypes = ['racing', 'track', 'cross', 'mountain'];
const statusTypes = ['Available', 'Taken'];
const bikes = [];
for (let i = 0; i < 10; i++) {
    bikes.push({
        id: i + 1,
        name: bikeSampleNames[getRandomInt(0, bikeSampleNames.length - 1)] + " " + (i + 1),
        type: bikeTypes[getRandomInt(0, bikeTypes.length - 1)],
        status: statusTypes[0]
    });
}

const router = new Router();
router.get('/bikes', ctx => {
    ctx.response.body = bikes;
    ctx.response.status = 200;
});

const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/register', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const type = headers.type;
    if (typeof name != 'undefined' && typeof type != 'undefined') {
        const index = bikes.findIndex(bike => bike.name === name && bike.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, bikes.map(function (bike) {
                    return bike.id;
                })) + 1;
            let bike = {
                id: maxId,
                name,
                type,
                status: statusTypes[0]
            };
            bikes.push(bike);
            broadcast(bike);
            ctx.response.body = bike;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'Bike already exists'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Missing name or type'};
        ctx.response.status = 404;
    }
});

router.post('/rent', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = bikes.findIndex(bike => bike.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let bike = bikes[index];
            if (bike.status != statusTypes[0]) {
                ctx.response.body = {text: 'The bike is already taken'};
                ctx.response.status = 404;
            } else {
                bike.status = statusTypes[1];
                ctx.response.body = bike;
                ctx.response.status = 200;
            }
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});

router.post('/release', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = bikes.findIndex(bike => bike.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let bike = bikes[index];
            bike.status = statusTypes[0];
            ctx.response.body = bike;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(3000);