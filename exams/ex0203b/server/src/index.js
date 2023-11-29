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

const carNames = ['John', 'Grand', 'Small', 'Big', 'Huge', 'Bad'];
const carModels = ['Audi', 'Bmw', 'Skoda', 'Seat', 'Tesla', 'Toyota'];
const statusTypes = ['old', 'new'];
const cars = [];
for (let i = 0; i < 10; i++) {
    cars.push({
        id: i + 1,
        name: carNames[getRandomInt(0, carNames.length - 1)],
        model: carModels[getRandomInt(0, carModels.length - 1)],
        status: statusTypes[1],
        year: getRandomInt(1990, 2018),
        km: getRandomInt(1, 3000)
    });
}

const router = new Router();
router.get('/cars', ctx => {
    ctx.response.body = cars;
    ctx.response.status = 200;
});

router.post('/modify', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const status = headers.status;
    const year = headers.year;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' &&
        typeof status !== 'undefined' && typeof year !== 'undefined') {
        const index = cars.findIndex(car => car.id == id);
        if (index === -1) {
            console.log("Car not available!");
            ctx.response.body = {text: 'Car not available!'};
            ctx.response.status = 404;
        } else {
            let car = cars[index];
            car.name = name;
            car.status = status;
            car.year = year;
            ctx.response.body = car;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or name or status or year!");
        ctx.response.body = {text: 'Missing or invalid: id or name or status or year!'};
        ctx.response.status = 404;
    }
});


router.post('/km', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const km = headers.km;
    if (typeof id !== 'undefined' && typeof km !== 'undefined') {
        const index = cars.findIndex(plane => plane.id == id);
        if (index === -1) {
            console.log("Car not available!");
            ctx.response.body = {text: 'Car not available!'};
            ctx.response.status = 404;
        } else {
            let car = cars[index];
            car.km = km;
            ctx.response.body = car;
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
    const model = headers.model;
    const year = headers.year;
    if (typeof name !== 'undefined' && typeof model !== 'undefined' &&
        typeof year !== 'undefined') {
        const index = cars.findIndex(car => car.name == name &&
        car.model == model && car.year == year);
        if (index !== -1) {
            console.log("Car already exists!");
            ctx.response.body = {text: 'Car already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, cars.map(function (car) {
                    return car.id;
                })) + 1;
            let car = {
                id: maxId,
                name,
                model,
                status: statusTypes[2],
                year,
                km: 0
            };
            cars.push(car);
            broadcast(car);
            ctx.response.body = car;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name or manufacturer or year!");
        ctx.response.body = {text: 'Missing or invalid: name or manufacturer or year!"'};
        ctx.response.status = 404;
    }
});

router.del('/car/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = cars.findIndex(car => car.id == id);
        if (index === -1) {
            console.log("No car with id: " + id);
            ctx.response.body = {text: 'Invalid car id'};
            ctx.response.status = 404;
        } else {
            let car = cars[index];
            cars.splice(index, 1);
            ctx.response.body = car;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4024);
