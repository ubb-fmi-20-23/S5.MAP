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

const names = ['Ionescu', 'Popescu', 'Vasilescu'];
const surnames = ['Vasile', 'Ion', 'Gheorghe'];
const groups = ['931', '935'];
const students = [];
for (let i = 0; i < 10; i++) {
    students.push({
        id: i + 1,
        name: names[getRandomInt(0, names.length - 1)] + " " + surnames[getRandomInt(0, surnames.length - 1)],
        group: groups[getRandomInt(0, groups.length - 1)],
        presences: 0
    });
}

const router = new Router();
router.get('/students', ctx => {
    ctx.response.body = students;
    ctx.response.status = 200;
});

const broadcast = (data) =>
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });

router.post('/presence', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id != 'undefined') {
        const index = students.findIndex(student => student.id === id);
        if (index === -1) {
            ctx.response.body = {text: 'Student not found'};
            ctx.response.status = 404;
        } else {
            console.log("found student: " + index);
            let student = students[index];
            student.presences++;
            broadcast(student);
            ctx.response.body = student;
            ctx.response.status = 200;
        }
    } else {
        ctx.response.body = {text: 'Missing id missing'};
        ctx.response.status = 404;
    }
});

router.post('/add', ctx => {
    const headers = ctx.request.body;
    console.log("body: " + JSON.stringify(headers));
    const name = headers.name;
    const group = headers.group;
    if (typeof name != 'undefined' && typeof group != 'undefined') {
        const index = students.findIndex(student => student.name === name && student.group === group);

        if (index === -1) {
            let maxId = Math.max.apply(Math, students.map(function (student) {
                    return student.id;
                })) + 1;
            let student = {
                id: maxId,
                name,
                group,
                presences: 0
            };
            students.push(student);
            ctx.response.body = student;
            ctx.response.status = 200;
        } else {
            ctx.response.body = {text: 'Missing student'};
            ctx.response.status = 404;
        }
    } else {
        ctx.response.body = {text: 'Name or group missing'};
        ctx.response.status = 404;
    }
});

app.use(router.routes());
app.use(router.allowedMethods());

server.listen(4000);