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

const airplaneNames = ['777', 'Mriya', 'a380', 'q400', 'rj145', '30mki', 'bomber', '340'];
const airplaneManufacturer = ['Aibus', 'Antonov', 'Boeing', 'Bombardier', 'Embraer', 'Sukhoi', 'Tupolev', 'Saab'];
const statusTypes = ['retired', 'in service', 'new'];
const airplanes = [];
for (let i = 0; i < 10; i++) {
    airplanes.push({
        id: i + 1,
        name: airplaneNames[getRandomInt(0, airplaneNames.length - 1)],
        manufacturer: airplaneManufacturer[getRandomInt(0, airplaneManufacturer.length - 1)],
        status: statusTypes[2],
        year: getRandomInt(1990, 2018),
        miles: getRandomInt(1, 2000)
    });
}

const router = new Router();
router.get('/airplanes', ctx => {
    ctx.response.body = airplanes;
    ctx.response.status = 200;
});

router.post('/update', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const status = headers.status;
    const year = headers.year;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' &&
        typeof status !== 'undefined' && typeof year !== 'undefined') {
        const index = airplanes.findIndex(airplane => airplane.id == id);
        if (index === -1) {
            console.log("Airplane not available!");
            ctx.response.body = {text: 'Airplane not available!'};
            ctx.response.status = 404;
        } else {
            let airplane = airplanes[index];
            airplane.name = name;
            airplane.status = status;
            airplane.year = year;
            ctx.response.body = airplane;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or name or status or year!");
        ctx.response.body = {text: 'Missing or invalid: id or name or status or year!'};
        ctx.response.status = 404;
    }
});


router.post('/miles', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const miles = headers.miles;
    if (typeof id !== 'undefined' && typeof miles !== 'undefined') {
        const index = airplanes.findIndex(plane => plane.id == id);
        if (index === -1) {
            console.log("Airplane not available!");
            ctx.response.body = {text: 'Airplane not available!'};
            ctx.response.status = 404;
        } else {
            let plane = airplanes[index];
            plane.miles = miles;
            ctx.response.body = plane;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or name or status or year!");
        ctx.response.body = {text: 'Missing or invalid: id or name or status or year!'};
        ctx.response.status = 404;
    }
});

const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/add', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const manufacturer = headers.manufacturer;
    const year = headers.year;
    if (typeof name !== 'undefined' && typeof manufacturer !== 'undefined' &&
        typeof year !== 'undefined') {
        const index = airplanes.findIndex(airplane => airplane.name == name &&
        airplane.manufacturer == manufacturer && airplane.year == year);
        if (index !== -1) {
            console.log("Airplane already exists!");
            ctx.response.body = {text: 'Airplane already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, airplanes.map(function (airplane) {
                    return airplane.id;
                })) + 1;
            let airplane = {
                id: maxId,
                name,
                manufacturer,
                status: statusTypes[2],
                year,
                miles: 0
            };
            airplanes.push(airplane);
            broadcast(airplane);
            ctx.response.body = airplane;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name or manufacturer or year!");
        ctx.response.body = {text: 'Missing or invalid: name or manufacturer or year!"'};
        ctx.response.status = 404;
    }
});

router.del('/airplane/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = airplanes.findIndex(airplane => airplane.id == id);
        if (index === -1) {
            console.log("No airplane with id: " + id);
            ctx.response.body = {text: 'Invalid airplane id'};
            ctx.response.status = 404;
        } else {
            let airplane = airplanes[index];
            airplanes.splice(index, 1);
            ctx.response.body = airplane;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4023);