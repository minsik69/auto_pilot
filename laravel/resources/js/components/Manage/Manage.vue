<template>
   <div class="container">
        <div class="left_container">
            <div class="category">   
                <b-card
                    header="카테고리"
                    header-tag="header"
                    style="max-width: 20rem;text-align:center;"
                    class="mb-2">
                    <div class="category_item_1">
                      <div style="text-align:center; color:gray; font-size:1.1em; margin-bottom:15px;">
                        정류장
                      </div>
                      <b-button class="button" @click="station_create()">
                        정류장 등록
                      </b-button>
                      <b-button class="button" @click="station_update()">
                        정류장 수정/삭제
                      </b-button>
                    </div>

                    <div class="category_item_2">
                      <div style="text-align:center; color:gray; font-size:1.1em; margin-bottom:15px; margin-top:15px;">
                        경로
                      </div>
                      <b-button class="button" @click="checkpoint_create()">
                        체크포인트 등록
                      </b-button>
                      <b-button class="button1" @click="checkpoint_update()">
                        체크포인트 수정/삭제
                      </b-button>
                      <b-button class="button" @click="path_create()">
                        경로 등록
                      </b-button>
                      <b-button class="button" @click="path_update()">
                        경로 수정/삭제
                      </b-button>
                    </div>

                    <div class="category_item_3">
                      <b-button class="button" @click="rc()">
                        RC카
                      </b-button>
                    </div>
                </b-card>
            </div>                    
        </div>  
        <div class="right_container">
          <div class="right_container_item1">
            <!-- map manage page -->
            <!-- id => 1 : station, path select -->
            <router-view v-if="id == 1"></router-view>  

            <!-- manage first page -->
            <!-- id => 0 : first page -->
            <b-col v-if="id == 0" style="line-height:550px; vertical-align:middle; left: 40%; position: absolute;">
             <font size="6em">원하는 카테고리를 선택해 주세요.</font>
            </b-col>

            <!-- rccar page -->
            <!-- id => 2 : rccar select -->
              <router-view v-if="id == 2"></router-view>  <!-- rc car -->
          </div>
        </div>
    </div>
</template>

<script>
export default {
  name: "Manage",
  data() {
    return {
      id : 0 // id => 0 : first page, 1 : station, path select, 2 : rccar select
    };
  },
  methods: {
    // station create function
    station_create() {
      this.id = 1
      this.$router.push({ name: "Manage_station_create" }).catch(err => {})
    },
    // station update function
    station_update() {
      this.id = 1
      this.$router.push({ name: "Manage_station_update" }).catch(err => {})
    },
    // path create function
    path_create() {
      this.id = 1
      this.$router.push({ name: "Manage_path_create" }).catch(err => {})
    },
    // path update function
    path_update() {
      this.id = 1
      this.$router.push({ name: "Manage_path_update" }).catch(err => {})
    },
    // rccar manage function
    rc() {
      this.id = 2
      this.$router.push({ name: "Manage_rc" }).catch(err => {})
    },
    // checkpoint create function
    checkpoint_create() {
      this.id = 1
      this.$router.push({ name: "Manage_checkpoint_create" }).catch(err => {})
    },
    // checkpoint update function
    checkpoint_update() {
      this.id = 1
      this.$router.push({ name: "Manage_checkpoint_update" }).catch(err => {})
    }
  }
};
</script>

<style scoped>
.container{
    display:grid;
    grid-template-columns: 16% 83%;
    width: 100%;
    max-width:1500px;
}

.category{            
  margin-top: 20px;
}

.category_item_1{ 
  border-bottom:2px solid #E6E9ED;
  padding: 10px;
  margin-right: 2%;
  margin-left: 2%;
  
}

.category_item_2{ 
  border-bottom:2px solid #E6E9ED;
  padding: 10px;
  margin-right: 2%;
  margin-left: 2%;
  
}
.button{ /* category button */
    color : black;
    background-color : white;
    border : none;
    margin-top : 5%;
}
.button1{ /* category checkpoint_update button */
   color : black;
    background-color : white;
    border : none;
    margin-top : 5%;
    font-size: 0.9em;
}

.right_container{  /* manage map */
  margin-top:20px;
  border:1px solid #E6E9ED;
  margin-left: 40px;
}
</style>
