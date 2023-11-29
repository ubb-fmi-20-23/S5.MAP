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

const itemsSample = ['Apples', 'Flowers', 'TV', 'Notebook', 'Milk', 'Water'];
const statusTypes = ['Pending', 'Purchased'];
const items = [];
for (let i = 0; i < 10; i++) {
    items.push({
        id: i + 1,
        name: itemsSample[getRandomInt(0, itemsSample.length - 1)],
        quantity: getRandomInt(5, 10),
        status: statusTypes[0]
    });
}

const router = new Router();
router.get('/items', ctx => {
    ctx.response.body = items;
    ctx.response.status = 200;
});

const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/buy', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = items.findIndex(item => item.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Item not found'};
            ctx.response.status = 404;
        } else {
            console.log("found item: " + index);
            let item = items[index];
            if (item.status != statusTypes[0]) {
                ctx.response.body = {text: 'Not a pending item'};
                ctx.response.status = 404;
            } else {
                console.log("Valid buy request of: " + JSON.stringify(item));
                item.status = statusTypes[1];
                broadcast(item);
                ctx.response.body = item;
                ctx.response.status = 200;
            }
        }
    } else {
        ctx.response.body = {text: 'Missing id'};
        ctx.response.status = 404;
    }
});

router.post('/add', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const quantity = headers.quantity;
    if (typeof name != 'undefined' && typeof quantity != 'undefined') {

        const index = items.findIndex(item => item.name === name && item.status === statusTypes[0]);

        if (index === -1) {
            let maxId = Math.max.apply(Math, items.map(function (item) {
                    return item.id;
                })) + 1;
            let item = {
                id: maxId,
                name,
                quantity,
                status: statusTypes[0]
            };
            items.push(item);
            ctx.response.body = item;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'Item already on the list'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Name or quantity missing'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(3000);