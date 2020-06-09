<template>
  <b-col align="right">
    <!-- RC car data table -->
    <b-table
      show-empty
      :fields="fields"
      :items="items"
      :per-page="perPage"
      :current-page="currentPage"
    >
      <!-- RC car product number -->
      <template v-slot:cell(car_num)="data">
        <!-- creation button click status -->
        {{ data.item.car_num == '' ? '' :  data.value }} 
        <!-- create an input window when registering -->
        <b-form-input 
          v-if="data.item.car_num == ''" 
          v-model="car_num" 
          :placeholder="update_car_num ? update_car_num : 'RC카 제품번호'">
        </b-form-input>
      </template>

      <!-- RC car name -->
      <template v-slot:cell(car_name)="data">
        <!-- enabling and disabling -->
        {{ data.item.car_name == update_car_name || data.item.car_name == '' ? '' : data.value }} 
        <!-- create an input window when registered or updated -->
        <b-form-input 
          v-if="data.item.car_name == update_car_name || data.item.car_name == ''" 
          v-model="car_name" 
          :placeholder="update_car_name ? update_car_name : 'RC카 이름'"></b-form-input>
      </template>

      <!-- RC car update button -->
      <template v-slot:cell(update)="data">
        <!-- create update input window -->
        <!-- create_id => 1 : create before 2 : creating update_id => 1 : update before 2 : updating -->
        <b-button  
          type="button" 
          @click="updateclick(data.item)" 
          variant="info" 
          v-if="create_id != 2 && update_id != 2">수정</b-button>
        <!-- updation completed -->
        <b-button 
          type="button" 
          @click="updateclick(data.item)" 
          variant="info" 
          v-if="create_id != 2 && data.item.car_num == update_car_num">수정 완료</b-button>
      </template>

      <!-- delete RC car, cancel button -->
      <template v-slot:cell(delete)="data">
          <!-- delete button -->
          <b-button 
            type="button" 
            @click="deleteclick(data.item.car_num)" 
            variant="danger" v-if="create_id != 2 && update_id != 2">삭제</b-button>
          <!-- cancel button while creating or updating -->
          <b-button 
            type="button" 
            @click="cancelclick()" 
            v-if="(create_id == 2 || update_id == 2) && (data.item.car_num == '' || data.item.car_num == update_car_num)">취소</b-button>
      </template>
    </b-table>
    <!-- pagenation -->
    <b-pagination
      v-model="currentPage"
      :total-rows="rows"
      :per-page="perPage"
      align="center"
    ></b-pagination>
    <!-- create button -->
    <!-- create_id => 1 : create before, 2 : creating -->
    <b-button 
      type="button" 
      @click="createclick()" 
      variant="success" 
      v-if="update_id != 2"> {{ create_id == 1 ? "등록" : "등록 완료"}}</b-button>
  </b-col>
</template>

<script>
  export default {
    mounted() {
      // load RC car data
      Axios.get('/api/dlvy/management/car')
      .then(res => {
        this.items = res.data.car_all
      })
    },
    data() {
      return {
        perPage: 5, //rows per page
        currentPage: 1,
        fields: [{
          key : 'car_num',
          label : 'car_num'
        },{
          key : 'car_name',
          label : 'car_name'
        },{
          key : 'update',
          label : 'update'
        },{
          key : 'delete',
          label : ''
        }],
        items: [], // RC car data
        create_id : 1, // 1: create before 2: create after 
        update_id : 1, // 1: update before 2: update after
        car_num : "", // RC car product number
        car_name : "", // RC car name
        update_car_num : "", // updated RC car product number
        update_car_name : "" // updated RC car name
      }
    },
    methods: {
      // RC car updation function
      updateclick(car) {
        // click the update RC car first button
        if(this.update_id == 1) {
          this.update_car_num = car.car_num
          this.update_car_name = car.car_name
          this.car_num = car.car_num
          this.car_name = car.car_name
          this.update_id += 1
        } 
        // click the update button after updating RC car data
        else if(this.update_id == 2) {
          // send RC car update data server
          Axios.patch(`/api/dlvy/management/car/${car.car_num}`, {
            car_name : this.car_name
          })
          .then(res => {
            this.update_id = 1
            this.update_car_num = ''
            this.update_car_name = ''
            this.car_num = ''
            this.car_name = ''
            this.items = res.data.car_all
          })
        }
      },
      // RC car deletion function
      deleteclick(car_num) {
        // send RC car delete number server
        Axios.delete(`/api/dlvy/management/car/${car_num}`)
        .then(res => {
          this.items = res.data.car_all // RCcar data
        })
      },
      // cancel click function
      cancelclick(){
        // initialize data
        if(this.items[this.items.length-1].car_num == "") {
          this.items.splice(this.items.length-1, 1)
        }
        
        this.update_id = 1
        this.create_id = 1
        this.update_car_num = ''
        this.update_car_name = ''
        this.car_num = ''
        this.car_name = ''
      },
      // RC car creation function
      createclick() {
        // click the RC car first creation button
        if(this.create_id == 1) {
          this.currentPage = Math.floor((this.items.length + 5) / 5) 
          this.items.push({
            car_num : "",
            car_name : "",
            car_status : "",
            car_lat : "",
            car_lon : "",
            car_error : ""
          })
          this.create_id += 1
        }
        // click the RC car second creation button
        else if (this.create_id == 2){
          // send RC car creation Data
          Axios.post('/api/dlvy/management/car', {
            car_num : this.car_num,
            car_name : this.car_name
          })
          .then(res => {
            this.create_id = 1
            this.items.splice(this.items.length-1, 1)
            this.items = res.data.car_all
            this.currentPage = Math.floor((this.items.length + 5) / 5)
            this.cancelclick()
          })
        }
      }
    },
    // return data length when RC car data is changed
    computed: {
      rows() {
        return this.items.length
      }
    }
  }
</script>