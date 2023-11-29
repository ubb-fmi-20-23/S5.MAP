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

const names = ['USD', 'RON', 'EURO', 'AUD', 'CAD', 'GBP', 'CNY', 'XAU'];
const descriptions = ['a thing', 'another thing', 'not useful', 'very useful', 'broken'];
const types = ['sell', 'buy'];
const statuses = ['open', 'closed'];
const orders = [];
for (let i = 0; i < 20; i++) {
    orders.push({
        id: i + 1,
        name: names[getRandomInt(0, names.length)],
        description: descriptions[getRandomInt(0, descriptions.length)],
        type: types[getRandomInt(0, types.length)],
        date: getRandomInt(2019, 2200),
        status: statuses[getRandomInt(0, statuses.length)],
        amount: getRandomInt(10, 200)
    });
}

const router = new Router();

router.get('/orders', ctx => {
    ctx.response.body = orders;
    ctx.response.status = 200;
});

router.get('/bySecurity/:name', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const name = headers.name;
    if (typeof name !== 'undefined') {
        ctx.response.body = orders.filter(order => order.name == name);
        ctx.response.status = 200;
    } else {
        console.log("Missing or invalid: name!");
        ctx.response.body = {text: 'Missing or invalid: name!'};
        ctx.response.status = 404;
    }
});

router.get('/order/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = orders.findIndex(order => order.id == id);
        if (index === -1) {
            console.log("Order not available!");
            ctx.response.body = {text: 'Order not available!'};
            ctx.response.status = 404;
        } else {
            let order = orders[index];
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id!");
        ctx.response.body = {text: 'Missing or invalid: id!'};
        ctx.response.status = 404;
    }
});

router.post('/order', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const description = headers.description;
    const type = headers.type;
    const date = headers.date;
    const status = headers.status;
    const amount = headers.amount;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' && typeof description !== 'undefined'
        && typeof type !== 'undefined' && typeof date !== 'undefined' && typeof status !== 'undefined'
        && typeof amount !== 'undefined') {
        const index = orders.findIndex(order => order.id == id);
        if (index === -1) {
            console.log("Order not available!");
            ctx.response.body = {text: 'Order not available!'};
            ctx.response.status = 404;
        } else {
            let order = orders[index];
            order.name = name;
            order.description = description;
            order.type = type;
            order.date = date;
            order.status = status;
            order.amount = amount;
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id, name, description, type, date, status or amount!");
        ctx.response.body = {text: 'Missing or invalid: id, name, description, type, date, status or amount!'};
        ctx.response.status = 404;
    }
});

router.post('/updateStatus', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const status = headers.status;
    if (typeof id !== 'undefined' && typeof status !== 'undefined') {
        const index = orders.findIndex(order => order.id == id);
        if (index === -1) {
            console.log("Order not available!");
            ctx.response.body = {text: 'Order not available!'};
            ctx.response.status = 404;
        } else {
            let order = orders[index];
            order.status = status;
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or status!");
        ctx.response.body = {text: 'Missing or invalid: id or status!'};
        ctx.response.status = 404;
    }
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
            order.status = statuses[1];
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id !");
        ctx.response.body = {text: 'Missing or invalid: id !'};
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


const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/create', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const description = headers.description;
    const type = headers.type;
    const date = headers.date;
    const status = headers.status;
    const amount = headers.amount;
    if (typeof name !== 'undefined' && typeof description !== 'undefined'
        && typeof type !== 'undefined' && typeof date !== 'undefined' && typeof status !== 'undefined'
        && typeof amount !== 'undefined') {
        const index = orders.findIndex(order => order.name == name);
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
                description,
                type,
                date,
                status,
                amount
            };
            orders.push(order);
            broadcast(order);
            ctx.response.body = order;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, description, type, date, status or amount!");
        ctx.response.body = {text: 'Missing or invalid: name, description, type, date, status or amount!'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2320);
