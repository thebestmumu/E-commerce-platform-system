import Vue from 'vue'
import VueRouter from 'vue-router'
import request from '../utils/request';

Vue.use(VueRouter)
//requireAuth: 是否需要检查登录
const routes = [
    //前台
  {
    path: '/',
    name: 'front',
    redirect: "/topview",
    component: () => import('../views/front/Front.vue'),
    meta: {title:'在线商城', path: '在线商城', requireAuth: false},
    children: [
      {path: 'person', name: 'person', meta: {title:'个人信息',requireLogin: true}, component: () => import('../views/Person.vue'),},
      {path: 'topview', name: 'topview', meta: {title:'在线商城'}, component: () => import('../views/front/TopView.vue'),},
      {path: 'cart', name: 'cart', meta: {title:'我的购物车',requireLogin: true}, component: () => import('../views/front/good/Cart.vue'),},
      {path: 'goodList', name: 'goodList', meta: {title:'商品界面'}, component: () => import('../views/front/good/GoodList.vue'),},
      {path: 'goodView/:goodId', name: 'goodview', meta: {title:'商品详情'}, component: () => import('../views/front/good/GoodView.vue'),},
      {path: 'preOrder', name: 'preOrder', meta: {title:'确认订单',requireLogin: true}, component: () => import('../views/front/order/PreOrder.vue'),},
      {path: 'pay/:orderId?', name: 'pay', meta: {title:'支付',requireLogin: true}, component: () => import('../views/front/order/Pay.vue'),},
      {path: 'pay-waiting', name: 'payWaiting', meta: {title:'等待支付',requireLogin: true}, component: () => import('../views/front/order/PayWaiting.vue'),},
      {path: 'pay-success', name: 'paySuccess', meta: {title:'支付成功',requireLogin: false}, component: () => import('../views/front/order/PaySuccess.vue'),},
      {path: 'orderList', name: 'orderList', meta: {title:'我的订单',requireLogin: true}, component: () => import('../views/front/order/OrderList.vue'),},
      {path: 'myTickets', name: 'myTickets', meta: {title:'我的工单',requireLogin: true}, component: () => import('../views/front/ticket/MyTickets.vue'),},

    ]
  },
    //后台
  {
    path: '/manage',
    name: 'manage',
    component: () => import('../views/manage/Manage.vue'),
    redirect: "/manage/home",
    meta: {title:'后台', path: '后台',requireAuth: true},
    children: [
      {path: 'home', name: 'home', meta: {title:'主页', path: '主页',requireAuth: true}, component: () => import('../views/manage/Home.vue'),},
      {path: 'user', name: 'user', meta: {title:'用户管理',path: '系统管理/用户管理',requireAuth: true}, component: () => import('../views/manage/User.vue'),},
      {path: 'person', name: 'person', meta: {title:'个人信息',path: '个人信息',requireAuth: true}, component: () => import('../views/Person.vue'),},
      {path: 'file', name: 'file', meta: {title:'文件管理',path: '文件/文件管理',requireAuth: true}, component: () => import('../views/manage/file/File.vue'),},
      {path: 'avatar', name: 'avatar', meta: {title:'头像管理',path: '文件/头像管理',requireAuth: true}, component: () => import('../views/manage/file/Avatar.vue'),},
      {path: 'carousel', name: 'carousel', meta: {title:'轮播图管理',path: '商品/轮播图管理',requireAuth: true}, component: () => import('../views/manage/good/Carousel.vue'),},
      {path: 'category', name: 'category', meta: {title:'商品分类管理',path: '商品/商品分类管理',requireAuth: true}, component: () => import('../views/manage/good/Category.vue'),},
      {path: 'good', name: 'good', meta: {title:'商品管理',path: '商品/商品管理',requireAuth: true}, component: () => import('../views/manage/good/Goods.vue'),},
      {path: 'goodInfo', name: 'goodInfo', meta: {title:'商品管理',path: '商品/商品管理/商品信息',requireAuth: true}, component: () => import('../views/manage/good/GoodInfo.vue'),},
      {path: 'order', name: 'order', meta: {title:'订单管理',path: '商品/订单管理',requireAuth: true}, component: () => import('../views/manage/Order.vue'),},
      {path: 'incomeChart', name: 'incomeChart', meta: {title:'收入图表',path: '营收/收入图表',requireAuth: true}, component: () => import('../views/manage/income/IncomeChart.vue'),},
      {path: 'incomeRank', name: 'incomeRank', meta: {title:'收入排行',path: '营收/收入排行',requireAuth: true}, component: () => import('../views/manage/income/IncomeRank.vue'),},

    ]
  },
  {
    path: '/login',
    name: 'login',
    meta: {
      title: '登录',
      requireAuth: false,
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/Login.vue')
  },
  {
    path: '/service-login',
    name: 'serviceLogin',
    meta: {
      title: '客服登录',
      requireAuth: false,
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/ServiceLogin.vue')
  },
  {
    path: '/service',
    name: 'service',
    meta: {
      title: '客服系统',
      requireAuth: false,  // 不使用 requireAuth，因为那是给普通用户和 admin 的
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/Service.vue'),
    children: [
      {
        path: 'ticket',
        name: 'serviceTicket',
        meta: {
          title: '工单处理',
          requireAuth: false,  // 客服系统有自己的验证逻辑
          requireService: true
        },
        component: () => import(/* webpackChunkName: "about" */ '../views/ServiceTicket.vue')
      },
      {
        path: 'chat',
        name: 'serviceChat',
        meta: {
          title: '在线客服',
          requireAuth: false,
          requireService: true
        },
        component: () => import(/* webpackChunkName: "about" */ '../views/ServiceChat.vue')
      }
    ]
  },
  {
    path: '/user-chat',
    name: 'userChat',
    meta: {
      title: '在线客服',
      requireAuth: false,
      requireUser: true
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/UserChat.vue')
  },
  {
    path: '/create-ticket',
    name: 'createTicket',
    meta: {
      title: '创建工单',
      requireAuth: false,
      requireUser: true
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/CreateTicket.vue')
  },
  {
    path: '/register',
    name: 'register',
    meta: {
      title: '注册',requireAuth: false,
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/Register.vue')
  },
  {
    path: '/*',
    name: 'notFound',
    meta: {
      title: '找不到页面'
    },
    component: () => import(/* webpackChunkName: "about" */ '../views/404NotFound.vue')
  },
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

//beforeEach是router的钩子函数，在进入路由前执行
router.beforeEach((to, from, next) => {
  let role;
  let allow = false;
  
  if(to.meta.requireAuth===true){
    request.post("http://localhost:9191/role").then(res=>{
      if(res.code==='200'){
        role = res.data;
        console.log('您的身份是：'+role);
        
        if(role === 'admin'){
          allow = true;
          if (to.meta.title) {
            document.title = to.meta.title
          } else {
            document.title ='未知页面'
          }
          next()
        }
        else if(role==='user'){
          if(to.path.startsWith('/manage')){
            alert("您没有权限访问后台");
            next("/topview")
          } else {
            if (to.meta.title) {
              document.title = to.meta.title
            } else {
              document.title ='未知页面'
            }
            next()
          }
        }
      }
      else{
        alert(res.msg);
        next('/login');
      }
    }).catch(() => {
      next('/login');
    })
  }
  else{
    if(to.path === '/manage' || to.path.startsWith('/manage/')){
      let user = localStorage.getItem("user");
      if(!user){
        next('/login');
        return;
      }
      try {
        const userData = JSON.parse(user);
        if(userData.role !== 'admin'){
          alert("您没有权限访问后台");
          next("/topview");
          return;
        }
      } catch(e) {
        next('/login');
        return;
      }
    }
    
    if(to.meta.requireLogin===true){
      if(!isLogin()){
        next('/login');
        return;
      }
    }
    
    // 检查是否需要客服权限
    if(to.meta.requireService===true){
      const serviceUserStr = localStorage.getItem('serviceUser')
      if(!serviceUserStr){
        next('/service-login');
        return;
      }
      try {
        const serviceUser = JSON.parse(serviceUserStr)
        if(!serviceUser.userId || serviceUser.role !== 'service'){
          next('/service-login');
          return;
        }
      } catch(e) {
        next('/service-login');
        return;
      }
    }
    
    if (to.meta.title) {
      document.title = to.meta.title
    } else {
      document.title ='未知页面'
    }
    next()
  }
})

function isLogin() {
  let user = localStorage.getItem("user");
  if(user){
    return true;
  }else{
    return false;
  }
}
export default router
