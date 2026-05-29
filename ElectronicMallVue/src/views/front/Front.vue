<!--
 * @Description: 
 * @Author: Rabbiter
 * @Date: 2023-03-26 15:27:05
-->
<template>
  <el-container class="front-container">
    <el-header class="front-header">
      <Navagation :user="user"
                  :role="role"
                  :login-status="loginStatus"
      ></Navagation>
    </el-header>

    <el-main class="front-main">
      <router-view />
    </el-main>

  </el-container>
</template>

<script>

import Navagation from "@/components/Navagation";
import request from "@/utils/request";

export default {
  name: "Front",
  data(){
    return{
      user:{},
      role: 'user',
      loginStatus: false,
    }
  },
  methods: {
    getUser() {
      let username = localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")).username : ""
      if (username) {
        // 从后台获取User数据
        this.request.get("/userinfo/" + username).then(res => {
          // 重新赋值后台的最新User数据
          this.user = res.data
          console.log(this.user.role)
        })
      }

    },
  },


  components:{
    Navagation,
  },
  created() {
    if(localStorage.getItem("user")){
      request.post("http://localhost:9191/role").then(res=> {
        if (res.code === '200') {
          this.role = res.data;
          if (localStorage.getItem("user")) {
            this.user = JSON.parse(localStorage.getItem("user"));
            this.loginStatus = true;
          }
        } else {
          this.user = {nickname: '您未登录', avatarUrl: null};
          localStorage.removeItem('user')
          this.loginStatus = false;
        }
      })
    }else{
      this.user = {nickname: '您未登录', avatarUrl: null};
      this.loginStatus = false;
    }

  }
}
</script>

<style scoped>
@import "../../resource/css/search.css";

.front-container {
  min-height: 100vh;
  width: 100%;
  background: #f4f4f4;
}

.front-header {
  background-color: white;
  padding: 0;
  height: 60px;
}

.front-main {
  background-color: #f4f4f4;
  padding: 0;
  width: 100%;
}

.image {
  width: 100%;
  display: block;
}
</style>