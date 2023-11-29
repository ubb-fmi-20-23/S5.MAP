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

const goodTypes = ['Oil', 'Bananas', 'Apples', 'Kiwis'];
const goods = [];
for (let i = 0; i < 10; i++) {
    goods.push({
        id: i + 1,
        name: goodTypes[getRandomInt(0, goodTypes.length - 1)],
        quantity: getRandomInt(5, 10),
        price: getRandomInt(20, 100)
    });
}

const router = new Router();
router.get('/goods', ctx => {
    ctx.response.body = goods;
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
    const name = headers.name;
    const quantity = headers.quantity;
    const price = headers.price;
    console.log("name: " + name + " quantity: " + quantity + " price: " + price);

    const index = goods.findIndex(g => g.name === name && g.quantity >= quantity && g.price <= price);
    if (index === -1) {
        ctx.response.body = {text: 'Good not found'};
        ctx.response.status = 404;
    } else {
        console.log("found item: " + index);
        let good = goods[index];
        good.quantity -= quantity;
        if (good.quantity === 0) {
            console.log("no more name: " + good.name);
            goods.splice(index, 1);
        }
        console.log("notify clients name: " + JSON.stringify(good));
        broadcast(good);
        ctx.response.body = good;
        ctx.response.status = 200;
    }
});

router.post('/sell', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const quantity = headers.quantity;
    const price = headers.price;
    console.log("name: " + name + " quantity: " + quantity + " price: " + price);

    let maxId = Math.max.apply(Math, goods.map(function (good) {
            return good.id;
        })) + 1;
    let good = {id: maxId, name, quantity, price};
    goods.push(good);
    ctx.response.body = good;
    ctx.response.status = 200;
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(3000);