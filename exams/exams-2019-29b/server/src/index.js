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
app.use(async (ctx, next) => {
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

const requestNames = ['Store1', 'Blue', 'Market13', 'Red', 'Billa'];
const productNames = ['Fish', 'Bread', 'Cereal', 'Milk', 'Soap', 'Pen'];
const statusTypes = ['open', 'closed', 'new'];
const requests = [];
for (let i = 0; i < 10; i++) {
    requests.push({
        id: i + 1,
        name: requestNames[getRandomInt(0, requestNames.length)],
        product: productNames[getRandomInt(0, productNames.length)],
        status: statusTypes[getRandomInt(0, statusTypes.length)],
        quantity: getRandomInt(1, 100)
    });
}

const router = new Router();
router.get('/requests', ctx => {
    ctx.response.body = requests.filter(request => request.status === statusTypes[0]);
    ctx.response.status = 200;
});

router.get('/closed', ctx => {
    ctx.response.body = requests.filter(request => request.status === statusTypes[1]);
    ctx.response.status = 200;
});

router.get('/big', ctx => {
    ctx.response.body = requests.filter(request => request.status === statusTypes[0] && request.quantity > 5);
    ctx.response.status = 200;
});

router.post('/fill', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = requests.findIndex(request => request.id == id);
        if (index === -1) {
            console.log("Request not available!");
            ctx.response.body = {text: 'Request not available!'};
            ctx.response.status = 404;
        } else {
            let request = requests[index];
            request.status = statusTypes[1];
            ctx.response.body = request;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id!");
        ctx.response.body = {text: 'Missing or invalid: id!'};
        ctx.response.status = 404;
    }
});


const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/request', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const product = headers.product;
    const quantity = headers.quantity;
    if (typeof name !== 'undefined' && typeof product !== 'undefined' && typeof quantity !== 'undefined') {
        const index = requests.findIndex(request => request.name == name);
        if (index !== -1) {
            console.log("Request already exists!");
            ctx.response.body = {text: 'Request already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, requests.map(function (request) {
                return request.id;
            })) + 1;
            let request = {
                id: maxId,
                name,
                product,
                quantity,
                status: statusTypes[0]
            };
            requests.push(request);
            broadcast(request);
            ctx.response.body = request;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, product or quantity!");
        ctx.response.body = {text: 'Missing or invalid: name, product or quantity!'};
        ctx.response.status = 404;
    }
});

router.del('/request/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = requests.findIndex(request => request.id == id);
        if (index === -1) {
            console.log("No request with id: " + id);
            ctx.response.body = {text: 'Invalid request id'};
            ctx.response.status = 404;
        } else {
            let place = requests[index];
            requests.splice(index, 1);
            ctx.response.body = place;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2229);
