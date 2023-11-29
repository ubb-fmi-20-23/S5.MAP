---
title: "Mapping Earthquake Data"
video_number: 57
date: 2017-02-13
video_id: ZiYdOwOrGyc
repository: /CC_57_Earthquake_Viz

links: 
  - title: "Mapbox.js"
    url: https://www.mapbox.com/mapbox.js/api/v3.0.1/
  - title: "Web Mercator on Wikipedia"
    url: https://en.wikipedia.org/wiki/Web_Mercator
  - title: "Earthquake Database"
    url: http://earthquake.usgs.gov/data/

videos:
  - title: "Videos on working with data and APIs"
    url: /Tutorials/10-working-with-data
  - title: "Regular Expression playlist"
    url: /Courses/programming-with-text/2-regular-expressions


contributions:
  - title: "TOR IP Map"
    author:
      name: "Lugsole"
      url: https://github.com/Lugsole
    url: https://lugsole.github.io/TOR_MAP/
    source: https://github.com/Lugsole/TOR_MAP
---

In this coding challenge, I visualize earthquake data from the [USGS](http://earthquake.usgs.gov/data/) by mapping the latitude, longitude and the magnitude of earthquakes with p5.js.

The map imagery is pulled from [mapbox.js](https://www.mapbox.com/mapbox.js/api/v3.0.1/) and the math demonstrated coverts latitude, longitude to x,y via [Web Mercator](https://en.wikipedia.org/wiki/Web_Mercator).
