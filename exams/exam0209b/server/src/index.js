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

const carSampleNames = ['Batmobile', 'Diamond', 'Fogger', 'Baby Blue'];
const carTypes = ['convertible', 'sports', 'truck', 'van'];
const statusTypes = ['Available', 'Taken'];
const cars = [];
for (let i = 0; i < 10; i++) {
    cars.push({
        id: i + 1,
        name: carSampleNames[getRandomInt(0, carSampleNames.length - 1)] + " " + (i + 1),
        type: carTypes[getRandomInt(0, carTypes.length - 1)],
        status: statusTypes[0]
    });
}

const router = new Router();
router.get('/cars', ctx => {
    ctx.response.body = cars;
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
        const index = cars.findIndex(car => car.name === name && car.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, cars.map(function (car) {
                    return car.id;
                })) + 1;
            let car = {
                id: maxId,
                name,
                type,
                status: statusTypes[0]
            };
            cars.push(car);
            broadcast(car);
            ctx.response.body = car;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'Car already exists'};
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
        const index = cars.findIndex(car => car.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let car = cars[index];
            if (car.status != statusTypes[0]) {
                ctx.response.body = {text: 'The car is already taken'};
                ctx.response.status = 404;
            } else {
                car.status = statusTypes[1];
                ctx.response.body = car;
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
        const index = cars.findIndex(car => car.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let car = cars[index];
            car.status = statusTypes[0];
            ctx.response.body = car;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4000);