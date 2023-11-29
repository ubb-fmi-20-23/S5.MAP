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

const securityNames = ['BTC', 'ETH', 'XRP', 'BCH', 'XLM', 'EOS', 'LTC', 'ADA'];
const typeNames = ['buy', 'sell'];
const statusTypes = ['open', 'closed'];
const orders = [];
for (let i = 0; i < 10; i++) {
    orders.push({
        id: i + 1,
        name: securityNames[getRandomInt(0, securityNames.length)],
        price: getRandomInt(1, 10000),
        quantity: getRandomInt(1, 100),
        type: typeNames[getRandomInt(0, typeNames.length)],
        status: statusTypes[getRandomInt(0, statusTypes.length)]
    });
}

const router = new Router();
router.get('/orders', ctx => {
    ctx.response.body = orders.filter(order => order.status === statusTypes[0]);
    ctx.response.status = 200;
});

router.get('/buy', ctx => {
    ctx.response.body = orders.filter(order => order.status === statusTypes[1] && order.type === typeNames[0]);
    ctx.response.status = 200;
});

router.get('/sell', ctx => {
    ctx.response.body = orders.filter(order => order.status === statusTypes[1] && order.type === typeNames[1]);
    ctx.response.status = 200;
});

router.post('/close', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = orders.findIndex(order => order.id == id);
        if (index === -1) {
            console.log("Order not available!");
            ctx.response.body = {text: 'Order not available!'};
            ctx.response.status = 404;
        } else {
            let order = orders[index];
            order.status = statusTypes[1];
            ctx.response.body = order;
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

router.post('/order', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const price = headers.price;
    const quantity = headers.quantity;
    const type = headers.type;
    if (typeof name !== 'undefined' && typeof price !== 'undefined' && typeof quantity !== 'undefined' && typeof type !== 'undefined') {
        const index = orders.findIndex(order => order.name == name && order.price == price && order.type == type);
        if (index !== -1) {
            console.log("Order already exists!");
            ctx.response.body = {text: 'Order already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, orders.map(function (order) {
                return order.id;
            })) + 1;
            let order = {
                id: maxId,
                name,
                price,
                quantity,
                type,
                status: statusTypes[0]
            };
            orders.push(order);
            broadcast(order);
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, price, quantity or type!");
        ctx.response.body = {text: 'Missing or invalid: name, price, quantity or type!'};
        ctx.response.status = 404;
    }
});

router.del('/order/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = orders.findIndex(order => order.id == id);
        if (index === -1) {
            console.log("No order with id: " + id);
            ctx.response.body = {text: 'Invalid order id'};
            ctx.response.status = 404;
        } else {
            let order = orders[index];
            orders.splice(index, 1);
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing or invalid'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2030);
