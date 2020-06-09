<template>
  <div class="page-container">
    <div id="map"></div>
    <div id="manager">
      <!-- stage = 1 : checkpoint click --> 
      <div v-if="stage == 1">지도에서 등록할 체크포인트를 클릭해주세요.</div>
      <!-- stage = 2 : data input -->
      <div v-if="stage == 2">
        <b-form>
          <!-- checkpoint latitude, longitude -->
          <div style="margin: 5px;">
            <div>
              <span style="font-size: 13px">위도 : {{ lat }}</span>
            </div>
            <div>
              <span style="font-size: 13px">경도 : {{ lon }}</span>
            </div>
          </div>
          <!-- chk_create() : checkpoint create function, initialize() : cancel function -->
          <b-button-group>
            <b-button type="button" variant="primary" @click="chk_create()">등록하기</b-button>
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
    // checkpoint load data
    Axios.get('/api/dlvy/management/checkpoint')
    .then(res => {
      this.data = res.data.checkpoint_all
      this.data.push({
        checkpoint_id: "",
        checkpoint_lat: "",
        checkpoint_lon: ""
      })
      this.initMap()
    }) 
  },
  data() {
    return {
      map_stage: 1, // kakaomap create limit (map_stage = 1 : create map, map_stage = 2 : Do not create maps)
      stage: 1, // step-by-step screen stage = 1 : station click, stage = 2 : create data input
      checkpoint_id: "", // checkpoint id
      lat: "", // latitude
      lon: "", // longitude
      data: "", // checkpoint data
      map: "", // map
      markers: [] // marker array
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
      // marker image
      var imageSrc = "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png"
      // create all markers
      for (let i = 0, len = this.data.length; i < len; i++) {
        // marker image size, path
        var imageSize = new kakao.maps.Size(24, 35);
        var markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize)
        // create marker
        const marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(this.data[i].checkpoint_lat, this.data[i].checkpoint_lon) ? new kakao.maps.LatLng(this.data[i].checkpoint_lat, this.data[i].checkpoint_lon) : "", // marker show coordinate
          image: markerImage,
        })
        // infowindow create
        var infowindow = new kakao.maps.InfoWindow({
          content:
            "<div style='text-align:center; margin-left:5px; color:#18a2b8'>" +
            "checkpoint id : " + (this.data[i].checkpoint_id ? this.data[i].checkpoint_id : "") +
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
      }
      // map click event
      kakao.maps.event.addListener(this.map, "click", (mouseEvent) => {
        let latlng = mouseEvent.latLng
        this.markers[this.markers.length-1].setPosition(latlng)
        this.lat = latlng.getLat()
        this.lon = latlng.getLng()
        // next screen when clicking map
        if(this.stage == 1)
          this.stage += 1
      })
    },
    // checkpoint create function
    chk_create() {
      // send to the checkpoint creation data server
      Axios.post('/api/dlvy/management/checkpoint', {
        checkpoint_id : this.checkpoint_id,
        checkpoint_lat : this.lat,
        checkpoint_lon : this.lon
      })
      .then(res => {
        this.data = res.data.checkpoint_all // checkpoint data
        this.initialize()
      })
      .catch(err => {
        console.log(err)
      })
    },
    // data initialization
    initialize() {
      // marker delete
      for (let i = 0, len = this.data.length; i < len; i++) {
        this.markers[i].setMap(null)
      }
      this.stage = 1
      this.lat = ""
      this.lon = ""
      this.checkpoint_id = ""
      this.markers = []
      // checkpoint array data initialization
      if (this.data[this.data.length - 1].checkpoint_id == "") {
        this.data.splice(this.data.length - 1, 1)
        this.data.push({
          checkpoint_id: "",
          checkpoint_lat: "",
          checkpoint_lon: ""
        })
      } 
      else if(this.data[this.data.length - 1].checkpoint_id != "" ) {
        this.data.push({
          checkpoint_id: "",
          checkpoint_lat: "",
          checkpoint_lon: ""
        })
      }
      this.initMap()
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