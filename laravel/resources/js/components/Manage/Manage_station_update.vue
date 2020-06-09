<template>
  <div class="page-container">
    <div id="map"></div>
    <div id="manager">
      <!-- stage = 1 : station click --> 
      <div v-if="stage == 1">지도에서 수정/삭제할 정류장을 클릭해 주세요.</div>
      <!-- stage = 2 : update data input -->
      <div v-if="stage == 2">
        <b-form>
          <p>정류장 이름 : {{ station_name }}</p>
          <!-- station name input -->
          <b-form-input 
            v-model="station_name"
            :placeholder="station_name"
          ></b-form-input>
          <!-- station latitude, longitude -->
          <div style="margin: 5px;">
            <div>
              <span style="font-size: 13px">위도 : {{ lat }}</span>
            </div>
            <div>
              <span style="font-size: 13px">경도 : {{ lon }}</span>
            </div>
          </div>
          <!-- old_station_name : station name before update -->
          <!-- stn_update() : station update function, stn_delete() : station delete function, initialize() : cancel function -->
          <b-button-group>
            <b-button type="button" variant="info" @click="stn_update(old_station_name)">수정하기</b-button>
            <b-button variant="danger" type="button" @click="stn_delete(old_station_name)">삭제하기</b-button>
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
    // station load data
    Axios.get('/api/dlvy/management/station')
    .then(res => {
      this.data = res.data.station_all
      this.initMap()
    }) 
  },
  data() {
    return {
      map_stage: 1, // kakaomap create limit (map_stage = 1 : create map, map_stage = 2 : Do not create maps)
      stage: 1, // step-by-step screen stage = 1 : station click, stage = 2 : update data input
      old_station_name: "", // station name before update
      station_name: "", // station name after update
      lat: "", // latitude
      lon: "", // longitude
      data: "", // station data
      markers: [], // marker array
      map: "", // map
      stationMarker: new kakao.maps.MarkerImage('/image/station.png', new kakao.maps.Size(30,30)) // station marker
    }
  },
  methods: {
    // infowindow open function
    makeOverListener(map, marker, infowindow) {
      return function() {
        infowindow.open(map, marker)
      }
    },
    // infowindow close function
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
      // create all markers
      if (this.stage == 1) {
        for (let i = 0, len = this.data.length; i < len; i++) {
          // create marker
          const marker = new kakao.maps.Marker({
            position: new kakao.maps.LatLng(this.data[i].station_lat, this.data[i].station_lon), // marker show coordinate
            image: this.stationMarker
          })
          // infowindow create
          var infowindow = new kakao.maps.InfoWindow({
            content:
              "<div style='text-align:center; margin-left:5px; color:#18a2b8'>" +
              (this.data[i].station_name) +
              "</div>",
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
          // marker create, marker array push
          marker.setMap(this.map)
          this.markers.push(marker)
          // map click event
          kakao.maps.event.addListener(this.markers[i], "click", () => {
            this.stage = 2 // create data input
            this.markers[i].setDraggable(true)
            this.old_station_name = this.data[i].station_name
            this.station_name = this.data[i].station_name
            this.lat = this.data[i].station_lat
            this.lon = this.data[i].station_lon
          })
          // marker drag end event
          kakao.maps.event.addListener(this.markers[i], "dragend", () => {
            this.lat = this.markers[i].getPosition().Ha
            this.lon = this.markers[i].getPosition().Ga
          })
        }
      }
    },
    // station data delete function
    stn_delete(station_name) {
      // send to the station delete data server
      Axios.delete(`/api/dlvy/management/station/${station_name}`)
      .then(res => {
        this.initialize(1) // marker delete
        this.data = res.data.station_all // station data
        this.initialize(2) // data initialization
      })
      .catch(err => {
        console.log(err)
      })
    },
    // station data update function
    stn_update(station_name) {
      // send to the station update data server
      Axios.patch(`/api/dlvy/management/station/${station_name}`, {
        station_name : this.station_name,
        station_lat : this.lat,
        station_lon : this.lon
      })
      .then(res => {
        this.initialize(1) // marker delete
        this.data = res.data.station_all // station data
        this.initialize(2) // data initialization
      })
      .catch(err => {
        console.log(err)
      })
    },
    // data initialization
    initialize(num) {
      // marker delete, marker initialize num = 1 : marker delete , num = 2 : data initialize
      if(num == 1) {
        for (let i = 0, len = this.data.length; i < len; i++) {
          this.markers[i].setMap(null)
        }
      } else {
        this.stage = 1
        this.station_name = ""
        this.old_station_name = ""
        this.lat = ""
        this.lon = ""
        this.markers = []
        this.initMap()
      }
    },
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