<template>
  <div class="page-container">
    <div id="map"></div>
    <div id="manager">
      <!-- stage = 1 : two station click -->
      <div v-if="stage == 1">경로를 등록할 두 정류장을 클릭해 주세요.</div>
      <!-- stage = 2 : path checkpoint click -->
      <div v-if="stage == 2">체크포인트 클릭 후 확인 버튼 클릭해 주세요. <br> 
      <!-- check() : checkpoint path create -->
      <b-button type="button" variant="primary" @click="check()">확인</b-button> </div>
      <!-- stage = 3 : path create information -->
      <div v-if="stage == 3">
        <b-form>
          <!-- two station name -->
          {{station_all[station_start].station_name}} ↔ {{station_all[station_end].station_name}}
          <div>체크포인트 수 : {{ checkpoint_num }}</div>
          <div>총 거리 : {{ distance }} m</div>
          <!-- path_create() : path create function, initialize() : cancel function -->
          <b-button-group>
            <b-button type="button" variant="primary" @click="path_create()">등록하기</b-button>
            <b-button type="button" @click="initialize()">취소하기</b-button>
          </b-button-group>
        </b-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  mounted() {
    // station, checkpoint load data
    Axios.get('/api/dlvy/management/path')
    .then(res => {
      this.station_all = res.data.station_all
      this.checkpoint_all = res.data.checkpoint_all
      this.initMap()
    })
  },
  data() {
    return {
      map_stage: 1, // kakaomap create limit (map_stage = 1 : create map, map_stage = 2 : Do not create maps)
      stage: 1, // step-by-step screen stage = 1 : two station click, stage = 2 : checkpoint click, stge = 3 : path information 
      station_all: "", // station data
      checkpoint_all: "", // checkpoint data
      station_start: "", // building1 station
      station_end: "", // building2 station
      station_markers: [], // station marker array
      checkpoint_markers: [], // checkpoint marker array
      checkpoint_markers_click: [], // checkpoint click marker array
      checkpoint_markers_clicked: [], // do not click checkpoint
      checkpoint_markers_clicknumber: [], // checkpoint click order created
      checkpoint_markers_id: [], // checkpoint click id array
      station_stage: 1, // station click station_stage = 1 : one station, station_stage = 2 : two station, station_stage = 3 : checkpoint show
      checkpoint_num: 0, // checkpoint click number
      overlay_data: [], // all overlay data
      distance: 0, // distance
      polylines: [], // polyline array
      linepath: [], // coordinate array of lines
      map: "", // map
      stationMarker: new kakao.maps.MarkerImage('/image/station.png', new kakao.maps.Size(30,30)) // station marker
    }
  },
  methods: {
    // two coordinate distance calculation function
    getDistance(lat1, lon1, lat2, lon2) {
      var R = 6371
      var dLat = this.deg2rad(lat2 - lat1)
      var dLon = this.deg2rad(lon2 - lon1)
      var a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(this.deg2rad(lat1)) *
          Math.cos(this.deg2rad(lat2)) *
          Math.sin(dLon / 2) *
          Math.sin(dLon / 2)
      var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
      var d = R * c
      return d // two coordinate distance
    },
    // two coordinate distance equation
    deg2rad(deg) {
      return deg * (Math.PI / 180)
    },
    // infowindow open event
    makeOverListener(map, marker, infowindow) {
      return function() {
        infowindow.open(map, marker)
      }
    },
    // infowindow close event
    makeOutListener(infowindow) {
      return function() {
        infowindow.close()
      }
    },
    // load map and kakao api events
    initMap() {
      // load kakao map
      if(this.map_stage == 1) {
        var container = document.getElementById("map");
        var options = {
          center: new kakao.maps.LatLng(35.896309, 128.621917), // map center latitude, longitude
          level: 2, // map zoom
          draggable: false, // stop moving the map
        };
        this.map = new kakao.maps.Map(container, options) // map settings
        this.map_stage = 2
      }
      // checkpoint click stop array
      for(let i = 0, len = this.checkpoint_all.length; i < len; i++) {
        this.checkpoint_markers_clicked.push(1)
      }
      // checkpoint marker image
      var imageSrc = "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png"
      // create all markers
      for (let i = 0, len = this.station_all.length; i < len; i++) {
        // create marker
        const marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(this.station_all[i].station_lat, this.station_all[i].station_lon), // marker show coordinate
          image: this.stationMarker
        })
        // infowindow create
        var infowindow = new kakao.maps.InfoWindow({
          content:
            "<div style='text-align:center; margin-left:5px; color:#18a2b8'>" +
            (this.station_all[i].station_name) +
            "</div>",
        })
        // station marker create, station marker array push
        marker.setMap(this.map)
        this.station_markers.push(marker)
        // station marker click event
        kakao.maps.event.addListener(this.station_markers[i], "click", () => {
          if(this.station_stage != 4)
            this.station_stage += 1
          // two stop marker click data 
          if (this.station_stage == 2) {
            this.station_start = i // click first station
            this.station_custom_overlay() // click station custom overlay
          } else if (this.station_stage == 3) {
            this.station_end = i // click two station 
            this.station_delete() // delete marker excluding click stop
            this.station_custom_overlay() // click station custom overlay
            this.checkpoint_start() // checkpoint show
            this.stage = 2 // checkpoint click screen
          }
        })
        // infowindow open event
        kakao.maps.event.addListener(
          marker,
          "mouseover",
          this.makeOverListener(this.map, marker, infowindow)
        )
        // infowindow close event
        kakao.maps.event.addListener(
          marker,
          "mouseout",
          this.makeOutListener(infowindow)
        )
      }
    },
    // station overlay function
    station_custom_overlay() {
      // two station
      if (this.station_stage == 2) {
        // custom overlay
        const content =
          "<div style='margin-bottom:36px;'>" +
          "  <span style='font-size:20px; font-weight:bold; color:red'>건물1</span>" +
          "</div>";
        // set station custom overlay
        let customOverlay = new kakao.maps.CustomOverlay({
          position: new kakao.maps.LatLng(this.station_all[this.station_start].station_lat, this.station_all[this.station_start].station_lon),
          content: content,
          yAnchor: 1,
        });
        // station custom overlay, array push
        customOverlay.setMap(this.map)
        this.overlay_data.push(customOverlay)
      }
      // two station
      if (this.station_stage == 3) {
        // custom overlay
        const content =
          "<div style='margin-bottom:36px;'>" +
          "  <span style='font-size:20px; font-weight:bold; color:red'>건물2</span>" +
          "</div>";
        // set station custom overlay
        let customOverlay = new kakao.maps.CustomOverlay({
          position: new kakao.maps.LatLng(this.station_all[this.station_end].station_lat, this.station_all[this.station_end].station_lon),
          content: content,
          yAnchor: 1,
        });
        // station custom overlay show, array push
        customOverlay.setMap(this.map)
        this.overlay_data.push(customOverlay)
      }
    },
    // checkpoint show
    checkpoint_start() {
      // checkpoint marker image
      var imageSrc = "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png";
      // create all checkpoint markers
      for (let i = 0; i < this.checkpoint_all.length; i++) {
        // checkpoint image size and path
        var imageSize = new kakao.maps.Size(24, 35);
        var markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize);
        // create checkpoint marker
        const marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(this.checkpoint_all[i].checkpoint_lat, this.checkpoint_all[i].checkpoint_lon), // 마커를 표시할 위치
          image: markerImage,
        });
        // infowindow create
        var infowindow = new kakao.maps.InfoWindow({
          content:
            "<div style='text-align:center; margin-left:5px; color:#18a2b8'>" +
            (this.checkpoint_all[i].checkpoint_id) +
            "</div>",
        });
        // checkpoint marker show, marker array push
        marker.setMap(this.map);
        this.checkpoint_markers.push(marker);
        // checkpoint click event
        kakao.maps.event.addListener(this.checkpoint_markers[i], "click", () => {
          // checkpoint operation
          if(this.checkpoint_markers_clicked[i] == 1) { // Do not double-click checkpoints
            this.checkpoint_markers_clicknumber.push(i) // create checkpoint click order
            this.checkpoint_markers_id.push(this.checkpoint_all[i].checkpoint_id) // push click checkpoint id
            this.checkpoint_num += 1 // checkpoint clicks
            this.checkpoint_custom_overlay(this.checkpoint_num, i) // show clicked checkpoint overlays
            this.checkpoint_markers_click.push(marker) // push checkpoint click marker array
            this.checkpoint_markers_clicked[i] += 1 
          }
        });
        // infowindow open event
        kakao.maps.event.addListener(
          marker,
          "mouseover",
          this.makeOverListener(this.map, marker, infowindow)
        );
        // infowindow close event
        kakao.maps.event.addListener(
          marker,
          "mouseout",
          this.makeOutListener(infowindow)
        );
      }
    },
    // checkpoint overlay function
    checkpoint_custom_overlay(num, i) {
        // show custom overlay click checkpoint marker
        const content =
        "<div style='margin-bottom:36px;'>" +
        "  <span style='font-size:20px; font-weight:bold; color:red'>" +
        (num) +
        "</span>" +
        "</div>";
        // click checkpoint location
        const position = new kakao.maps.LatLng(
          this.checkpoint_all[i].checkpoint_lat,
          this.checkpoint_all[i].checkpoint_lon
        )
        // set checkpoint custom overlay
        let customOverlay = new kakao.maps.CustomOverlay({
          position: position,
          content: content,
          yAnchor: 1,
        });
        // checkpoint custom overlay display, array push
        customOverlay.setMap(this.map)
        this.overlay_data.push(customOverlay)
    },
    // function executed by clicking two station
    station_delete() {
      // Delete all stations except clicked stations
      for (let i = 0, len = this.station_markers.length; i < len; i++) {
        if(this.station_start == i || this.station_end == i)
          continue
        this.station_markers[i].setMap(null)
      }
    },
    // distance calculation, path display function
    check() {
      if(this.checkpoint_markers.length > 0) {
        // stage = 3 : path information
        this.stage = 3
        // delete all checkpointmarkers
        for(let i = 0, len = this.checkpoint_markers.length; i < len; i++) {
          this.checkpoint_markers[i].setMap(null)
        }
        // click checkpoint show
        for(let i = 0, len = this.checkpoint_markers_click.length; i < len; i++) {
          this.checkpoint_markers_click[i].setMap(this.map)
        }
        // two stations, click checkpoint coordinate array  
        this.linepath = []
        // total distance calculation
        for(let i = 0, len = this.checkpoint_markers_clicknumber.length; i < len; i++) {
          var checkpoint_lat = this.checkpoint_all[this.checkpoint_markers_clicknumber[i]].checkpoint_lat
          var checkpoint_lon = this.checkpoint_all[this.checkpoint_markers_clicknumber[i]].checkpoint_lon
          if(len-1 != i) {
            var checkpoint_lat_plus = this.checkpoint_all[this.checkpoint_markers_clicknumber[i+1]].checkpoint_lat
            var checkpoint_lon_plus = this.checkpoint_all[this.checkpoint_markers_clicknumber[i+1]].checkpoint_lon
          }
          if(i == 0) {
            var station_one_lat = this.station_all[this.station_start].station_lat
            var station_one_lon = this.station_all[this.station_start].station_lon
            var station_two_lat = this.station_all[this.station_end].station_lat
            var station_two_lon = this.station_all[this.station_end].station_lon
            this.distance = this.distance + this.getDistance(station_one_lat, station_one_lon, checkpoint_lat, checkpoint_lon)
            this.linepath.push(new kakao.maps.LatLng(station_one_lat, station_one_lon))
            this.linepath.push(new kakao.maps.LatLng(checkpoint_lat, checkpoint_lon))
            if(this.checkpoint_markers_clicknumber.length == 1) {
              this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, station_two_lat, station_two_lon)
              this.linepath.push(new kakao.maps.LatLng(station_two_lat, station_two_lon))
            } else {
              this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, checkpoint_lat_plus, checkpoint_lon_plus)
              this.linepath.push(new kakao.maps.LatLng(checkpoint_lat_plus, checkpoint_lon_plus))
            }
            continue
          } else if(i == this.checkpoint_markers_clicknumber.length - 1) {
            this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, station_two_lat, station_two_lon)
            this.linepath.push(new kakao.maps.LatLng(station_two_lat, station_two_lon))
            continue
          }
          this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, checkpoint_lat_plus, checkpoint_lon_plus)
          this.linepath.push(new kakao.maps.LatLng(checkpoint_lat, checkpoint_lon))
          this.linepath.push(new kakao.maps.LatLng(checkpoint_lat_plus, checkpoint_lon_plus))
        }
 
        // polyline settings
        const polyline = new kakao.maps.Polyline({
            path: this.linepath,
            strokeWeight: 5,
            strokeColor: '#FFAE00',
            strokeOpacity: 0.7, 
            strokeStyle: 'solid'
        });
        // polyline show, array push
        polyline.setMap(this.map)
        this.polylines.push(polyline)
        // distance unit m
        this.distance = this.distance.toFixed(3) * 1000
      }
    },
    // path create function
    path_create() {
      // send to the path creation data server
      Axios.post('/api/dlvy/management/path', {
        checkpoint_id: this.checkpoint_markers_id, // click checkpoint array
        path_start_point: this.station_all[this.station_start].station_name, // one station name
        path_end_point: this.station_all[this.station_end].station_name, // two station name
      })
      .then(res => {
        this.initialize()
      })
      .catch(err => {
        console.log(err)
      })
    },
    // data initialization
    initialize() {
      // polyline array delete
      this.polylines[0].setMap(null)
      // delete all overlays
      for(let i = 0, len = this.overlay_data.length; i < len; i++) {
        this.overlay_data[i].setMap(null)
      }
      // delete all checkpoint marker
      for(let i = 0, len = this.checkpoint_markers_click.length; i < len; i++) {
        this.checkpoint_markers_click[i].setMap(null)
      }
      // delete all station marker
      for(let i = 0, len = this.station_markers.length; i < len; i++) {
        this.station_markers[i].setMap(null)
      }
     
      this.stage = 1 
      this.station_start = ""
      this.station_end = ""
      this.station_markers = []
      this.checkpoint_markers = []
      this.checkpoint_markers_click = []
      this.checkpoint_markers_clicked = []
      this.checkpoint_markers_clicknumber = []
      this.checkpoint_markers_id = []
      this.station_stage = 1
      this.checkpoint_num = 0
      this.overlay_data = []
      this.distance = 0
      this.linepath = []
      this.polylines = []
      this.initMap()
    }
  },
};
</script>

<style scoped>
#map {
  height: 600px;
}
#manager {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 10;
  background-color: white;
  border: 1px solid #18a2b8;
  padding: 10px;
  text-align: center;
}
.page-container {
  position: relative;
}
</style>