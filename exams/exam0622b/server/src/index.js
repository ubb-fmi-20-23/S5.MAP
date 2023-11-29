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

const productType = ['book', 'food', 'toy', 'dairy'];
const statusTypes = ['Available', 'Sold'];
const products = [];
for (let i = 0; i < 10; i++) {
    products.push({
        id: i + 1,
        name: "Product " + (i + 1),
        quantity: getRandomInt(1,10),
        type: productType[getRandomInt(0, productType.length - 1)],
        status: statusTypes[0]
    });
}

const router = new Router();
router.get('/all', ctx => {
    ctx.response.body = products;
    ctx.response.status = 200;
});

router.get('/products', ctx => {
    ctx.response.body = products.filter(product => product.status === 'Available');
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
    const quantity = headers.quantity;
    if (typeof name != 'undefined' && typeof type != 'undefined' && typeof quantity != 'undefined') {
        const index = products.findIndex(product => product.name === name && product.type === type);
        if (index === -1) {
            let maxId = Math.max.apply(Math, products.map(function (product) {
                    return product.id;
                })) + 1;
            let product = {
                id: maxId,
                name,
                type,
                status: statusTypes[0],
                quantity
            };
            products.push(product);
            broadcast(product);
            ctx.response.body = product;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'The product already exists'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Missing name, type and quantity fields'};
        ctx.response.status = 405;
    }
});

router.delete('/remove/:id', ctx => {
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = products.findIndex(product => product.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            products.splice(index, 1);
            ctx.response.body = {text: 'The product was deleted'};
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
    const quantity = headers.quantity;
    if (typeof id != 'undefined' && typeof name != 'undefined' && typeof type != 'undefined' && typeof quantity != 'undefined') {
        const index = products.findIndex(product => product.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'The product is missing'};
            ctx.response.status = 404;
        } else {
            let product = products[index];
            product.name = name;
            product.type = type;
            product.quantity = quantity;
            ctx.response.body = product;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Missing id, name, type or quantity field'};
        ctx.response.status = 405;
    }
});


router.post('/buy', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = products.findIndex(product => product.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let product = products[index];
            if (product.status != statusTypes[0]) {
                ctx.response.body = {text: 'The product is already taken'};
                ctx.response.status = 404;
            } else {
                product.status = statusTypes[1];
                ctx.response.body = product;
                ctx.response.status = 200;
            }
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 405;
    }
});

router.delete('/return/:id', ctx => {
    const headers = ctx.params;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = products.findIndex(product => product.id == id);
        if (index === -1) {
            ctx.response.body = {text: 'Invalid id'};
            ctx.response.status = 404;
        } else {
            let product = products[index];
            product.status = statusTypes[0];
            ctx.response.body = product;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Id missing'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(3300);
