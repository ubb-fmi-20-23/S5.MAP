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

const names = ['Jack', 'John', 'Clyde', 'David', 'Morgan', 'George', 'Michael', ''];
const groups = [911, 921, 931, 912, 922, 932, 913, 923, 933, 914, 924, 934];
const years = [2017, 2018, 2019, 2020];
const statuses = ['free', 'paid'];
const courseNames = ['SO', 'MA', 'PBT', 'AT', 'OOP'];
const students = [];
for (let i = 0; i < 10; i++) {
    students.push({
        id: i + 1,
        name: names[getRandomInt(0, names.length)],
        group: groups[getRandomInt(0, groups.length)],
        year: years[getRandomInt(0, years.length)],
        status: statuses[getRandomInt(0, statuses.length)],
        credits: getRandomInt(0, 200)
    });
}

const lectures = [];

let lectureId = 0;
for (const course in courseNames) {
    for (let i = 0; i < 14; i++) {
        lectures.push({
            id: ++lectureId,
            name: "Lecture " + courseNames[course] + " " + lectureId
        });
    }
}

const router = new Router();

router.get('/students', ctx => {
    ctx.response.body = students;
    ctx.response.status = 200;
});

router.get('/lectures', ctx => {
    ctx.response.body = lectures;
    ctx.response.status = 200;
});


router.get('/byLecture/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const id = headers.id;
    if (typeof id !== 'undefined') {
        ctx.response.body = students.filter(student => student.lecture == id);
        ctx.response.status = 200;
    } else {
        console.log("Missing or invalid: lecture id!");
        ctx.response.body = {text: 'Missing or invalid: lecture id!'};
        ctx.response.status = 404;
    }
});

router.get('/student/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = students.findIndex(student => student.id == id);
        if (index === -1) {
            console.log("Student not available!");
            ctx.response.body = {text: 'Student not available!'};
            ctx.response.status = 404;
        } else {
            ctx.response.body = students[index];
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id!");
        ctx.response.body = {text: 'Missing or invalid: id!'};
        ctx.response.status = 404;
    }
});

router.post('/student', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const name = headers.name;
    const group = headers.group;
    const year = headers.year;
    const status = headers.status;
    const credits = headers.credits;
    if (typeof id !== 'undefined' && typeof name !== 'undefined' && typeof group !== 'undefined'
        && typeof year !== 'undefined' && typeof status !== 'undefined'
        && typeof credits !== 'undefined') {
        const index = students.findIndex(student => student.id == id);
        if (index === -1) {
            console.log("Student not available!");
            ctx.response.body = {text: 'Student not available!'};
            ctx.response.status = 404;
        } else {
            let student = students[index];
            student.name = name;
            student.group = group;
            student.year = year;
            student.status = status;
            student.credits = credits;
            ctx.response.body = student;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id, name, group, year, status or credits!");
        ctx.response.body = {text: 'Missing or invalid: id, name, group, year, status or credits!'};
        ctx.response.status = 404;
    }
});

router.post('/updateCredit', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    const credits = headers.credits;
    if (typeof id !== 'undefined' && typeof credits !== 'undefined') {
        const index = students.findIndex(student => student.id == id);
        if (index === -1) {
            console.log("Student not available!");
            ctx.response.body = {text: 'Student not available!'};
            ctx.response.status = 404;
        } else {
            let student = students[index];
            student.credits = credits;
            ctx.response.body = student;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: id or credits!");
        ctx.response.body = {text: 'Missing or invalid: id or credits!'};
        ctx.response.status = 404;
    }
});

router.post('/register', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.request.body;
    // console.log("body: " + JSON.stringify(headers));
    const lectureId = headers.lectureId;
    const studentId = headers.studentId;
    if (typeof lectureId !== 'undefined' && typeof studentId !== 'undefined') {
        const indexStudent = students.findIndex(student => student.id == studentId);
        if (indexStudent === -1) {
            console.log("Student not available!");
            ctx.response.body = {text: 'Student not available!'};
            ctx.response.status = 404;
        } else {
            const indexLecture = lectures.findIndex(lecture => lecture.id == lectureId);
            if (indexLecture === -1) {
                console.log("Lecture not available!");
                ctx.response.body = {text: 'Lecture not available!'};
                ctx.response.status = 404;
            } else {
                let student = students[indexStudent];
                student.lecture = lectureId;
                ctx.response.body = student;
                ctx.response.status = 200;
            }
        }
    } else {
        console.log("Missing or invalid: lecture id or student id!");
        ctx.response.body = {text: 'Missing or invalid: lecture id or student id!'};
        ctx.response.status = 404;
    }
});


router.del('/student/:id', ctx => {
    // console.log("ctx: " + JSON.stringify(ctx));
    const headers = ctx.params;
    // console.log("body: " + JSON.stringify(headers));
    const id = headers.id;
    if (typeof id !== 'undefined') {
        const index = students.findIndex(student => student.id == id);
        if (index === -1) {
            console.log("No student with id: " + id);
            ctx.response.body = {text: 'Invalid student id'};
            ctx.response.status = 404;
        } else {
            let student = students[index];
            students.splice(index, 1);
            ctx.response.body = student;
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
    const group = headers.group;
    const year = headers.year;
    const status = headers.status;
    if (typeof name !== 'undefined' && typeof group !== 'undefined'
        && typeof year !== 'undefined' && typeof status !== 'undefined') {
        const index = students.findIndex(student => student.name == name);
        if (index !== -1) {
            console.log("Student already exists!");
            ctx.response.body = {text: 'Student already exists!'};
            ctx.response.status = 404;
        } else {
            let maxId = Math.max.apply(Math, students.map(function (student) {
                return student.id;
            })) + 1;
            let student = {
                id: maxId,
                name,
                group,
                year,
                status,
                credits: 0
            };
            students.push(student);
            broadcast(student);
            ctx.response.body = student;
            ctx.response.status = 200;
        }
    } else {
        console.log("Missing or invalid: name, group, year or status!");
        ctx.response.body = {text: 'Missing or invalid: name, group, year or status!'};
        ctx.response.status = 404;
    }
});


app.use(router.routes());
app.use(router.allowedMethods());

server.listen(2021);
