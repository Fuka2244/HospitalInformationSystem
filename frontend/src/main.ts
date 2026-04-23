import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import './styles/global.css'
import App from './App.vue'
import router from './router'
import FadeIn from './directives/fadeIn'
import Parallax from './directives/parallax'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// global directives
app.directive('fade-in', FadeIn)
app.directive('parallax', Parallax)

app.mount('#app')
