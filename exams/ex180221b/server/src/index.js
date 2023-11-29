const koa = require('koa');
const app = new koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({server});
const Router = require('koa-router');
const cors = require('koa-cors');
const bodyParser = require('koa-bodyparser');
const convert = require('koa-convert');

app.use(bodyParser());
app.use(convert(cors()));
app.use(async(ctx, next) => {
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

const boatNames = ['Yellow', 'Blue', 'Small', 'Big', 'Lincoln'];
const boatModels = ['Serenity', 'Orion', 'Whisper'];
const statusTypes = ['free', 'busy'];
const boats = [];
for (let i = 0; i < 10; i++) {
    boats.push({
        id: i + 1,
        name: boatNames[getRandomInt(0, boatNames.length)],
        model: boatModels[getRandomInt(0, boatModels.length )],
        status: statusTypes[getRandomInt(0, statusTypes.length )],
        seats: getRandomInt(1, 10),
        rides: getRandomInt(1, 1000)
    });
}

const router = new Router();
router.get('/boats', ctx => {
    ctx.response.body = boats.filter(boat => boat.status == statusTypes[0]);
    ctx.response.status = 200;
});

router.get('/busy', ctx => {
    ctx.response.body = boats.filter(boat => boat.status == statusTypes[1]);
    ctx.response.status = 200;
});


router.post('/change', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const status = headers.status;
    const seats = headers.seats;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' &&
        typeof status !== 'undefined' && typeof seats !== 'undefined') {
        const index = boats.findIndex(boat => boat.id == id);
        if (index === -1) {
            console.log("Boat not available!");
            ctx.response.body = {text: 'Boat not available!'};
            ctx.response.status = 404;
        } else {
            let boat = boats[index];
            boat.name = name;
            boat.status = status;
            boat.seats = seats;
            ctx.response.body = boat;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or name or status or seats!");
        ctx.response.body = {text: 'Missing or invalid: id or name or status or seats!'};
        ctx.response.status = 404;
    }
});


router.post('/rides', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const rides = headers.rides;
    if (typeof id !== 'undefined' && typeof rides !== 'undefined') {
        const index = boats.findIndex(boat => boat.id == id);
        if (index === -1) {
            console.log("Boat not available!");
            ctx.response.body = {text: 'Boat not available!'};
            ctx.response.status = 404;
        } else {
            let boat = boats[index];
            boat.rides = boat.rides + rides;
            ctx.response.body = boat;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or rides!");
        ctx.response.body = {text: 'Missing or invalid: id or rides!'};
        ctx.response.status = 404;
    }
});

const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/new', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const model = headers.model;
    const seats = headers.seats;
    if (typeof name !== 'undefined' && typeof model !== 'undefined' &&
        typeof seats !== 'undefined') {
        const index = boats.findIndex(boat => boat.name == name &&
        boat.model == model && boat.seats == seats);
        if (index !== -1) {
            console.log("Boat already exists!");
            ctx.response.body = {text: 'Boat already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, boats.map(function (boat) {
                    return boat.id;
                })) + 1;
            let boat = {
                id: maxId,
                name,
                model,
                status: statusTypes[0],
                seats,
                km: 0
            };
            boats.push(boat);
            broadcast(boat);
            ctx.response.body = boat;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name or model or rides!");
        ctx.response.body = {text: 'Missing or invalid: name or model or rides!"'};
        ctx.response.status = 404;
    }
});

router.del('/boat/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = boats.findIndex(boat => boat.id == id);
        if (index === -1) {
            console.log("No boat with id: " + id);
            ctx.response.body = {text: 'Invalid boat id'};
            ctx.response.status = 404;
        } else {
            let boat = boats[index];
            boats.splice(index, 1);
            ctx.response.body = boat;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4022);
