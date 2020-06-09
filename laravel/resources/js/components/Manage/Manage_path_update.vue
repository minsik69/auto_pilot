<template>
  <div class="page-container">
    <div id="map"></div>
    <div id="manager">
      <!-- stage = 1 : click update path station -->
      <div v-if="stage == 1">경로를 수정/삭제할 정류장을 클릭해 주세요.</div>
      <!-- stage = 2 : click update path -->
      <div v-if="stage == 2">수정/삭제를 원하는 경로를 클릭해 주세요.</div>
      <div
        v-show="stage == 2"
        style="border-top: 1px solid #18a2b8; margin-top: 10px"
      >
        <div style="margin-top:10px; margin-bottom: 10px">현재 등록된 경로</div>
        <!-- path_one_all : all paths to the station, path_one : clicked path data -->
        <b-list-group v-for="path_one in path_one_all" :key="path_one.id">
          <b-list-group-item>
            <!-- path_click() : path data import function -->
            <h5 style="cursor:pointer" @click="path_click(path_one)">
                {{ path_one.path_start_point }} ↔ {{ path_one.path_end_point }}
            </h5>
            <!-- information about the path -->
            <div v-if="path_one.path_id == path_check"> 
            <h6>체크포인트 수 : {{ checkpoint_num }}</h6>
            <h6>총 거리 : {{ distance }} m</h6>
              <div style="margin-top:10px; margin-bottom: 0">
                <!-- path_update() : path update function -->
                <b-button
                  variant="info"
                  type="button"
                  @click="path_update(path_one)">수정하기</b-button>
                <!-- path_delete() : path delete function, path_one.path_id : path primary_key -->
                <b-button 
                  type="button" 
                  variant="danger" 
                  @click="path_delete(path_one.path_id)">삭제하기</b-button>
                <!-- initialize() : data initialization -->
                <b-button 
                  type="button" 
                  @click="initialize(1)">취소하기</b-button>
              </div>
            </div>
          </b-list-group-item>
        </b-list-group>
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
      this.initMap();
    })
  },
  data() {
    return {
      stage: 1, // step-by-step screen stage = 1 : station click, stage = 2 : path click, information
      map_stage: 1, // kakaomap create limit (map_stage = 1 : create map, map_stage = 2 : Do not create maps)
      station_all: "", // station data
      checkpoint_all: "", // checkpoint data
      path_one_all: "", // all path data
      path_start_point: [], // click to station related path data
      checkpoint_sequence: [], // checkpoint sequence, id
      checkpoint_markers_clicked: [], // click stop
      station_markers: [], // marker save
      checkpoint_markers_all: [], // save all markers
      overlay_data: [], // all overlay data
      polylines: [], // polyline array 
      linepath: [], // coordinate array of lines
      path_check: "", // path differentiation
      station_stop: 1, // Do not click on the station
      station_end_point: "", // delete when clicking on another path
      checkpoint_num: 0, // checkpoint number
      checkpoint_update_num: 0, // checkpoint update sequence
      distance: 0, // distance
      checkpoint_stop: 0, // checkpoint create stop
      map: "", // map
      stationMarker: new kakao.maps.MarkerImage('/image/station.png', new kakao.maps.Size(30,30)) // station marker
    };
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
        }
        this.map = new kakao.maps.Map(container, options) // map settings
        this.map_stage = 2
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
          if(this.station_stop == 1) {
            if(this.stage != 2)
                this.stage += 1
            // import from all path servers for click station
            Axios.get(`/api/dlvy/management/path/${this.station_all[i].station_name}`)
            .then(res => {
                this.path_one_all = res.data // all paths of station
            })
            .catch(err => {
                console.log(err)
            })
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
    // checkpoint show function
    checkpoint_start(list) {
      // checkpoint marker image
      var imageSrc = "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png"
      // create all markers
      for (let i = 0, len = this.checkpoint_all.length; i < len; i++) {
        // marker image size, path
        var imageSize = new kakao.maps.Size(24, 35);
        var markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize);
        // create marker
        const marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(this.checkpoint_all[i].checkpoint_lat, this.checkpoint_all[i].checkpoint_lon), // 마커를 표시할 위치
          image: markerImage,
        })
        // infowindow create
        var infowindow = new kakao.maps.InfoWindow({
          content:
            "<div style='text-align:center; margin-left:5px; color:#18a2b8'>" +
            (this.checkpoint_all[i].checkpoint_id) +
            "</div>",
        });
        // do not click checkpoint
        for(let i = 0, len = this.checkpoint_all.length; i < len; i++) {
          this.checkpoint_markers_clicked.push(1)
        }
        // checkpoint marker create, marker array push
        marker.setMap(this.map)
        this.checkpoint_markers_all.push(marker)
        // imported checkpoints
        if(this.checkpoint_sequence.length > i) {
          this.checkpoint_custom_overlay() // output corresponding list checkpoint order
          this.check(i) // show distance, polyline
        }
        // checkpoint click marker event
        kakao.maps.event.addListener(this.checkpoint_markers_all[i], "click", () => {
            // import a clicked checkpoint
            let click_check = this.checkpoint_sequence.filter((item) => {
              return item.check_id === this.checkpoint_all[i].checkpoint_id
            })
            // Initialize checkpoint to click
            if(click_check.length == 0) {
              click_check.push({
                check_id: "",
                sequence: ""
              })
            }
            // checkpoint delete
            if(click_check[0].check_id == this.checkpoint_all[i].checkpoint_id) { 
              // checkpoint number, custom overlay, sequence data array
              let indexnumber = this.checkpoint_sequence.findIndex((item) => item.check_id == this.checkpoint_all[i].checkpoint_id)
              let overlay_data_copy = Array.prototype.slice.call(this.overlay_data)
              let checkpoint_sequence_copy = Array.prototype.slice.call(this.checkpoint_sequence)
              // delete checkpoint overlay
              for(let i = indexnumber, len = this.checkpoint_sequence.length; i < len; i++) {
                this.checkpoint_update_num -= 1
                this.checkpoint_num = this.checkpoint_update_num
                this.overlay_data[i].setMap(null)
              }
              // overlay data after deletion, setting checkpoint order array
              overlay_data_copy.splice(indexnumber,this.checkpoint_sequence.length-1)
              this.checkpoint_sequence.splice(indexnumber, this.checkpoint_sequence.length)
              this.overlay_data = overlay_data_copy
            } 
            // add checkpoint 
            else { 
              // checkpoint sequence, adding custom overlays
              click_check.splice(click_check.length, 1)
              this.checkpoint_sequence.push({ check_id : this.checkpoint_all[i].checkpoint_id, sequence : this.checkpoint_update_num + 1 }) 
              this.checkpoint_custom_overlay(this.checkpoint_update_num) 
              this.checkpoint_num = this.checkpoint_update_num
            }
            // delete checkpoint polyline
            for(let i = 0, len = this.polylines.length; i < len; i++) {
              this.polylines[i].setMap(null)
            } 
            // coordinate array, initializing distance
            this.linepath = []
            this.distance = 0
            // set checkpoint path after deletion
            for(let i = 0, len = this.checkpoint_sequence.length; i < len; i++) {
              this.check(i)
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
    // checkpoint overlay function
    checkpoint_custom_overlay() {
      // custom overlay number
      this.checkpoint_update_num = this.checkpoint_update_num + 1
      // custom overlay content
      const content =
      "<div style='margin-bottom:36px;'>" +
      "  <span style='font-size:20px; font-weight:bold; color:red'>" +
      (this.checkpoint_update_num) +
      "</span>" +
      "</div>"
      // click checkpoint id
      const coordinate = this.checkpoint_all.filter((item) => {
        return item.checkpoint_id === this.checkpoint_sequence[this.checkpoint_update_num - 1].check_id
      })
      // set custom overlay location
      const position = new kakao.maps.LatLng(
        coordinate[0].checkpoint_lat,
        coordinate[0].checkpoint_lon
      )
      // custom overlay settings
      let customOverlay = new kakao.maps.CustomOverlay({
        position: position,
        content: content,
        yAnchor: 1,
      })
      // show custom overlay, push array
      customOverlay.setMap(this.map)
      this.overlay_data.push(customOverlay)
    },
    // Delete Station function except clicked stop
    station_delete(start_point, end_point) {
      for (let i = 0; i < this.station_markers.length; i++) {
        if(start_point == this.station_all[i].station_name || end_point == this.station_all[i].station_name)
          continue
        this.station_markers[i].setMap(null)
      }
    },
    // distance calculation, polyline display
    check(point) { 
      if(point != 0) {
        // checkpoint distance comparison
        var checkpoint_markers_previous = this.checkpoint_all.filter((item) => {
          return item.checkpoint_id === this.checkpoint_sequence[point-1].check_id
        }) 
        // checkpoint coordinates
        var chk_prev_lat = checkpoint_markers_previous[0].checkpoint_lat
        var chk_prev_lon = checkpoint_markers_previous[0].checkpoint_lon
      }
      // click checkpoint id, coordinate
      var checkpoint_markers_clicked = this.checkpoint_all.filter((item) => {
        return item.checkpoint_id === this.checkpoint_sequence[point].check_id
      }) 
      var checkpoint_lat = checkpoint_markers_clicked[0].checkpoint_lat
      var checkpoint_lon = checkpoint_markers_clicked[0].checkpoint_lon
      // Check two stops
      var station_clicked = this.path_one_all.filter((item) => {
        return item.path_id === this.path_check}) 
      // first station coordinates
      var station_start = this.station_all.filter((item) => {
        return item.station_name === station_clicked[0].path_start_point
      }) 
      // second station coordinates
      var station_end = this.station_all.filter((item) => {
        return item.station_name === station_clicked[0].path_end_point
      }) 
      // clicked two station coordinates
      let stn_start_lat = station_start[0].station_lat
      let stn_start_lon = station_start[0].station_lon
      let stn_end_lat = station_end[0].station_lat
      let stn_end_lon = station_end[0].station_lon
      // checkpoint distance from the first station
      if(point == 0) {
        this.distance = this.distance + this.getDistance(stn_start_lat,stn_start_lon,checkpoint_lat,checkpoint_lon).toFixed(3) * 1000
        this.linepath.push(new kakao.maps.LatLng(stn_start_lat, stn_start_lon))
        this.linepath.push(new kakao.maps.LatLng(checkpoint_lat, checkpoint_lon))
        if(this.checkpoint_sequence.length == 1) {
          this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, stn_end_lat, stn_end_lon).toFixed(3) * 1000
          this.linepath.push(new kakao.maps.LatLng(stn_end_lat, stn_end_lon))
        }
      } 
      // checkpoint-to-checkpoint distance
      else if(point == this.checkpoint_sequence.length - 1) {
        this.distance = this.distance + this.getDistance(chk_prev_lat, chk_prev_lon, checkpoint_lat, checkpoint_lon).toFixed(3) * 1000
        this.distance = this.distance + this.getDistance(checkpoint_lat, checkpoint_lon, stn_end_lat, stn_end_lon).toFixed(3) * 1000
        this.linepath.push(new kakao.maps.LatLng(checkpoint_lat, checkpoint_lon))
        this.linepath.push(new kakao.maps.LatLng(stn_end_lat, stn_end_lon))
      } 
      // checkpoint and second stop distance
      else {
        this.distance = this.distance + this.getDistance(chk_prev_lat, chk_prev_lon, checkpoint_lat, checkpoint_lon).toFixed(3) * 1000
        this.linepath.push(new kakao.maps.LatLng(checkpoint_lat, checkpoint_lon))
      }
      // polyline settings
      if(point == this.checkpoint_sequence.length - 1) {
        const polyline = new kakao.maps.Polyline({
            path: this.linepath, 
            strokeWeight: 5, 
            strokeColor: '#FFAE00', 
            strokeOpacity: 0.7, 
            strokeStyle: 'solid' 
        })
        // polyline show, array push
        polyline.setMap(this.map)
        this.polylines.push(polyline)
      }
    },
    // list click function
    path_click(path_one) {
      // prevent checkpoint creation
      this.checkpoint_stop += 1
      if(this.checkpoint_stop >= 2)
        this.initialize(2)
      // distinguish clicked lists
      this.path_check = path_one.path_id
      this.station_stop = 2 // do not click on the stop
      
      // load data related to path click id
      axios.get(`/api/dlvy/management/pathcheck/${this.path_check}`)
      .then(res => {
        this.checkpoint_num = res.data.length
        this.checkpoint_sequence = res.data 
        this.checkpoint_start(this.checkpoint_stop)
        this.station_delete(path_one.path_start_point, path_one.path_end_point)
      })
      .catch(err => {
        console.log(err)
      })
    },
    // path delete function
    path_delete(id) {
      // send path delete id to server
      Axios.delete(`/api/dlvy/management/path/${id}`)
      .then(res => {
        this.path_one_all = res.data
        this.initialize(1)
      })
      .catch(err => {
        console.log(err)
      })
    },
    // path update function
    path_update(id) {
      // checkpoint sequence, id
      let checkpoint_id = []
      for(let i = 0, len = this.checkpoint_sequence.length; i < len; i++) {
         checkpoint_id.push(this.checkpoint_sequence[i].check_id)
      }
      // checkpoint order, sending id to server
      Axios.patch(`/api/dlvy/management/path/${id.path_id}`, {
        checkpoint_id: checkpoint_id
      })
      .then(res => {
        this.path_one_all = res.data
        this.initialize(1)
      })
      .catch(err => {
        console.log(err)
      })
    },
    // data initialization
    initialize(id) {
      // delete all polylines
      for(let i = 0, len = this.polylines.length; i < len; i++) {
        this.polylines[i].setMap(null)
      }
      // delete all checkpoint marker
      for(let i = 0, len = this.checkpoint_markers_all.length; i < len; i++) {
        this.checkpoint_markers_all[i].setMap(null)
      }
      // delete all station marker
      for(let i = 0, len = this.station_markers.length; i < len; i++) {
        this.station_markers[i].setMap(null)
      }
      // delete all overlays
      for(let i = 0, len = this.overlay_data.length; i < len; i++) {
        this.overlay_data[i].setMap(null)
      }
      this.checkpoint_num = 0
      this.checkpoint_update_num = 0
      this.distance = 0
      this.station_stop = 1
      this.path_check = ""
      this.station_markers = []
      this.checkpoint_markers_clicked = []
      this.checkpoint_markers_all = []
      this.overlay_data = []
      this.polylines = []
      this.linepath = []
      if(id == 1)
        this.checkpoint_stop = 0
      this.initMap()
    }
  }
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