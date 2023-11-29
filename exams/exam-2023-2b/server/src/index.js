var koa = require('koa');
var app = module.exports = new koa();
const server = require('http').createServer(app.callback());
const WebSocket = require('ws');
const wss = new WebSocket.Server({ server });
const Router = require('koa-router');
const cors = require('@koa/cors');
const bodyParser = require('koa-bodyparser');

app.use(bodyParser());

app.use(cors());

app.use(middleware);

function middleware(ctx, next) {
  const start = new Date();
  return next().then(() => {
    const ms = new Date() - start;
    console.log(`${start.toLocaleTimeString()} ${ctx.response.status} ${ctx.request.method} ${ctx.request.url} - ${ms}ms`);
  });
}

const getRandomInt = (min, max) => {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min)) + min;
};

const activityNames = [
  "Strength Training",
  "Cardiovascular Exercise",
  "Yoga",
  "Pilates",
  "High-Intensity Interval Training (HIIT)",
  "Spinning",
  "Barre",
  "Functional Training",
  "CrossFit",
  "Bodyweight Training"
];

const descriptionNames = [
  "Build muscle and improve overall fitness with resistance training.",
  "Boost your heart health and burn calories with aerobic exercise.",
  "Improve flexibility, balance, and mental focus with yoga.",
  "Tone your muscles and improve core strength with Pilates.",
  "Challenge your whole body with high-intensity, short-duration workouts.",
  "Get a high-energy, indoor cycling workout.",
  "Strengthen and tone your muscles using a ballet-inspired workout.",
  "Train for real-life movements and activities with functional exercises.",
  "Improve your overall fitness with a combination of weightlifting, gymnastics, and cardio.",
  "Challenge your body and build strength with bodyweight exercises."
];

const categories = [
  "Strength Training",
  "Cardiovascular Exercise",
  "Mind-Body",
  "High-Intensity Training",
  "Cycling",
  "Barre and Dance",
  "Functional Training",
  "CrossFit",
  "Bodyweight Training",
  "Specialty Classes"
];

const dates = [
  new Date("2023-01-01"),
  new Date("2023-02-01"),
  new Date("2023-03-01"),
  new Date("2023-04-01"),
  new Date("2023-05-01"),
  new Date("2023-06-01"),
  new Date("2023-07-01"),
  new Date("2023-08-01"),
  new Date("2023-09-01"),
  new Date("2023-10-01")
];

const intensities = ['easy', 'medium', 'hard'];
const activities = [];
for (let i = 0; i < 20; i++) {
  activities.push({
    id: i + 1,
    name: activityNames[getRandomInt(0, activityNames.length)],
    description: descriptionNames[getRandomInt(0, descriptionNames.length)],
    category: categories[getRandomInt(0, categories.length)],
    date: dates[getRandomInt(0, dates.length)],
    time: getRandomInt(10, 100),
    intensity: intensities[getRandomInt(0, intensities.length)]
  });
}

const router = new Router();
router.get('/categories', ctx => {
  ctx.response.body = categories;
  ctx.response.status = 200;
});

router.get('/activities/:category', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.params;
  const category = headers.category;
  // console.log("category: " + JSON.stringify(category));
  ctx.response.body = activities.filter(activity => activity.category == category);
  // console.log("body: " + JSON.stringify(ctx.response.body));
  ctx.response.status = 200;
});

router.get('/easiest', ctx => {
  ctx.response.body = activities;
  ctx.response.status = 200;
});

router.post('/intensity', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  const intensity = headers.intensity;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  if (typeof id !== 'undefined' && typeof intensity !== 'undefined') {
    const index = activities.findIndex(activity => activity.id == id);
    if (index === -1) {
      const msg = "No activity with id: " + id;
      console.log(msg);
      ctx.response.body = { text: msg };
      ctx.response.status = 404;
    } else {
      let activity = activities[index];
      activity.difficulty = intensity;
      ctx.response.body = activity;
      ctx.response.status = 200;
    }
  } else {
    const msg = "Missing or invalid: id or intensity!";
    console.log(msg);
    ctx.response.body = { text: msg };
    ctx.response.status = 404;
  }
});

const broadcast = (data) =>
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(data));
    }
  });

router.post('/activity', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.request.body;
  // console.log("body: " + JSON.stringify(headers));
  const name = headers.name;
  const description = headers.description;
  const category = headers.category;
  const date = headers.date;
  const time = headers.time;
  const intensity = headers.intensity;
  if (typeof name !== 'undefined'
    && typeof description !== 'undefined'
    && typeof category !== 'undefined'
    && typeof date !== 'undefined'
    && typeof time !== 'undefined'
    && typeof intensity !== 'undefined') {
    const index = activities.findIndex(activity => activity.name == name);
    if (index !== -1) {
      const msg = "Activity already exists!";
      console.log(msg);
      ctx.response.body = { text: msg };
      ctx.response.status = 404;
    } else {
      let maxId = Math.max.apply(Math, activities.map(function (activity) {
        return activity.id;
      })) + 1;
      let activity = {
        id: maxId,
        name,
        description,
        date,
        time,
        category,
        intensity
      };
      activities.push(activity);
      broadcast(activity);
      ctx.response.body = activity;
      ctx.response.status = 200;
    }
  } else {
    const msg = "Missing or invalid: name, description, category, date, time or intensity! name: " + name + " description: " + description + " category: " + category + " date: " + date + " time: " + time + " intensity: " + intensity;
    console.log(msg);
    ctx.response.body = { text: msg };
    ctx.response.status = 404;
  }
});

router.del('/activity/:id', ctx => {
  // console.log("ctx: " + JSON.stringify(ctx));
  const headers = ctx.params;
  // console.log("body: " + JSON.stringify(headers));
  const id = headers.id;
  if (typeof id !== 'undefined') {
    const index = activities.findIndex(activity => activity.id == id);
    if (index === -1) {
      const msg = "No activity with id: " + id;
      console.log(msg);
      ctx.response.body = { text: msg };
      ctx.response.status = 404;
    } else {
      let activity = activities[index];
      activities.splice(index, 1);
      ctx.response.body = activity;
      ctx.response.status = 200;
    }
  } else {
    ctx.response.body = { text: 'Id missing or invalid' };
    ctx.response.status = 404;
  }
});

app.use(router.routes());
app.use(router.allowedMethods());

const port = 2302;

server.listen(port, () => {
  console.log(`🚀 Server listening on ${port} ... 🚀`);
});