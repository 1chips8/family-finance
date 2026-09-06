import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElDatePicker,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElDrawer,
  ElInput,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
} from 'element-plus'
import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/date-picker/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/pagination/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/table/style/css'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import './styles/main.css'
import './styles/monthly-report.css'
import './styles/data-pages.css'

const app = createApp(App).use(createPinia()).use(router)
app.component('ElDatePicker', ElDatePicker)
app.component('ElDialog', ElDialog)
app.component('ElDropdown', ElDropdown)
app.component('ElDropdownItem', ElDropdownItem)
app.component('ElDropdownMenu', ElDropdownMenu)
app.component('ElDrawer', ElDrawer)
app.component('ElInput', ElInput)
app.component('ElOption', ElOption)
app.component('ElPagination', ElPagination)
app.component('ElSelect', ElSelect)
app.component('ElTable', ElTable)
app.component('ElTableColumn', ElTableColumn)
app.mount('#app')
