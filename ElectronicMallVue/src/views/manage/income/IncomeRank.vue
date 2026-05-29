<!--
 * @Description: 
 * @Author: Rabbiter
 * @Date: 2023-03-26 15:27:05
-->
<template >
  <div style="width: 58%;height:100%;margin: 20px auto;">
    <span v-for="(good,index) in good" :key="index">
      <income-item :index="index+1" :good="good" :categories="categories" style="margin-bottom: 2px"></income-item>
    </span>
  </div>
</template>

<script>
import incomeItem from "@/components/IncomeItem";
export default {
  name: "IncomeRank",
  data(){
    return{
      num: 10,
      good: [],
      categories: [],
    }
  },
  components:{
    'income-item': incomeItem,
  },
  created() {
    //先查询分类id和名称
    this.request.get("/api/category").then(res=> {
      if (res.code === '200') {
        this.categories = res.data;
      }
      //获取排行数据
      this.request.get("/api/good/rank/",{params:{num: this.num}}).then(res=>{
        if(res.code==='200'){
          this.good = res.data;
        }
      })
    })

  },

}
</script>

<style scoped>

</style>