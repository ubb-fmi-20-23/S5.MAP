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

const transportType = ['car', 'bus', 'airplane', 'train'];
const statusTypes = ['Available', 'Unavailable'];
const trips = [];
for (let i = 0; i < 10; i++) {
    trips.push({
        id: i + 1,
        name: "Trip " + (i + 1),
        rooms: getRandomInt(1,10),
        type: transportType[getRandomInt(0, transportType.length - 1)],
        status: statusTypes[0]
    });
}

const router = new Router();
router.get('/all', ctx => {
    ctx.response.body = trips;
    ctx.response.status = 200;
});

router.get('/trips', ctx => {
    ctx.response.body = trips.filter(trip => trip.status === 'Available');
    ctx.response.status = 200;
});


const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/add', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const type = headers.type;
    const rooms = headers.rooms;
    if (typeof name != 'undefined' && typeof type != 'undefined' && typeof rooms != 'undefined') {
        const index = trips.findIndex(trip => trip.name === name && trip.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, trips.map(function (trip) {
                    return trip.id;
                })) + 1;
            let trip = {
                id: maxId,
                name,
                type,
                status: statusTypes[0],
                rooms
            };
            trips.push(trip);
            broadcast(trip);
            ctx.response.body = trip;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'The trip already exists'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Missing name, type and rooms fields'};
        ctx.response.status = 405;
    }
});

router.delete('/del/:id', ctx => {
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = trips.findIndex(trip => trip.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            trips.splice(index, 1);
            ctx.response.body = {text: 'The trip was deleted'};
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 405;
    }
});

router.post('/update', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const type = headers.type;
    const rooms = headers.rooms;
    if (typeof id != 'undefined' && typeof name != 'undefined' && typeof type != 'undefined' && typeof rooms != 'undefined') {
        const index = trips.findIndex(trip => trip.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'The trip is missing'};
            ctx.response.status = 404;
        } else {
            let trip = trips[index];
            trip.name = name;
            trip.type = type;
            trip.rooms = rooms;
            ctx.response.body = trip;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Missing id, name, type or rooms field'};
        ctx.response.status = 405;
    }
});


router.post('/book', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = trips.findIndex(trip => trip.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let trip = trips[index];
            if (trip.status != statusTypes[0]) {
                ctx.response.body = {text: 'The trip is already taken'};
                ctx.response.status = 404;
            } else {
                trip.status = statusTypes[1];
                ctx.response.body = trip;
                ctx.response.status = 200;
            }
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 405;
    }
});

router.delete('/cancel/:id', ctx => {
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = trips.findIndex(trip => trip.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let trip = trips[index];
            trip.status = statusTypes[0];
            ctx.response.body = trip;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(3200);