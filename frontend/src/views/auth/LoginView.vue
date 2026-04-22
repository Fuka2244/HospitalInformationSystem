<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="card-header">
          <h2>医院信息系统</h2>
          <p>{{ isLogin ? '用户登录' : '用户注册' }}</p>
        </div>
      </template>

      <!-- 登录表单 -->
      <el-form v-if="isLogin" ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="loginForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <el-form-item>
          <div class="link-row">
            <el-link type="primary" @click="isLogin = false">没有账号？去注册</el-link>
            <el-link type="warning" @click="showForget = true">忘记密码？</el-link>
          </div>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="registerForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="registerForm.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="registerForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="registerForm.idCard" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="isLogin = true">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 忘记密码对话框 -->
    <el-dialog v-model="showForget" title="忘记密码" width="460px">
      <el-form ref="forgetFormRef" :model="forgetForm" :rules="forgetRules" label-width="100px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="forgetForm.phone" placeholder="请输入注册手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="verificationCode">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input v-model="forgetForm.verificationCode" placeholder="请输入验证码" maxlength="6" style="flex: 1" />
            <el-button :disabled="codeCooldown > 0" :loading="codeLoading" @click="handleSendCode">
              {{ codeCooldown > 0 ? `${codeCooldown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgetForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgetForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForget = false">取 消</el-button>
        <el-button type="primary" :loading="loading" @click="handleForget">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { forgetPassword, sendVerificationCode } from '@/api/patient'
import type { LoginDto, RegisterDto, ForgetPasswordDto } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const isLogin = ref(true)
const showForget = ref(false)
const codeLoading = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// ===== 手机号校验 =====
const phoneValidator = (_rule: any, value: string, callback: any) => {
  if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

// ===== 身份证校验 =====
const idCardValidator = (_rule: any, value: string, callback: any) => {
  if (!/^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(value)) {
    callback(new Error('请输入正确的18位身份证号'))
  } else {
    callback()
  }
}

// ===== 登录 =====
const loginFormRef = ref<FormInstance>()
const loginForm = reactive<LoginDto>({ phone: '', password: '' })
const loginRules: FormRules<LoginDto> = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { validator: phoneValidator, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await loginFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// ===== 注册 =====
const registerFormRef = ref<FormInstance>()
const registerForm = reactive<RegisterDto>({
  name: '', username: '', password: '', confirmPassword: '',
  gender: '男', age: 25, phone: '', address: '', idCard: '',
})

const registerRules: FormRules<RegisterDto> = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { validator: phoneValidator, trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { validator: idCardValidator, trigger: 'blur' }],
}

async function handleRegister() {
  await registerFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    isLogin.value = true
    loginForm.phone = registerForm.phone
  } finally {
    loading.value = false
  }
}

// ===== 忘记密码 =====
const forgetFormRef = ref<FormInstance>()
const forgetForm = reactive<ForgetPasswordDto>({ phone: '', verificationCode: '', newPassword: '', confirmPassword: '' })
const forgetRules: FormRules<ForgetPasswordDto> = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { validator: phoneValidator, trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== forgetForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(forgetForm.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  codeLoading.value = true
  try {
    const res = await sendVerificationCode(forgetForm.phone)
    if (res.success) {
      ElMessage.success('验证码已发送')
      codeCooldown.value = 60
      cooldownTimer = setInterval(() => {
        codeCooldown.value--
        if (codeCooldown.value <= 0 && cooldownTimer) {
          clearInterval(cooldownTimer)
          cooldownTimer = null
        }
      }, 1000)
    } else {
      ElMessage.error(res.errorMsg || '发送失败')
    }
  } finally {
    codeLoading.value = false
  }
}

async function handleForget() {
  await forgetFormRef.value?.validate()
  loading.value = true
  try {
    await forgetPassword(forgetForm)
    ElMessage.success('密码修改成功，请登录')
    showForget.value = false
    isLogin.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background:
    radial-gradient(900px 450px at 20% 0%, rgba(47, 128, 237, 0.45), transparent 60%),
    radial-gradient(900px 450px at 100% 20%, rgba(120, 87, 255, 0.40), transparent 60%),
    linear-gradient(135deg, #0b1220 0%, #111b34 50%, #0f172a 100%);
  position: relative;
  overflow: hidden;
}
.login-container::before,
.login-container::after{
  content:"";
  position:absolute;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  filter: blur(0px);
  opacity: 0.55;
  pointer-events:none;
  transform: translate3d(0,0,0);
}
.login-container::before{
  left: -180px;
  top: -200px;
  background: radial-gradient(circle at 30% 30%, rgba(79, 172, 254, 1), rgba(0, 242, 254, 0) 62%);
}
.login-container::after{
  right: -220px;
  bottom: -220px;
  background: radial-gradient(circle at 30% 30%, rgba(250, 112, 154, 1), rgba(254, 225, 64, 0) 62%);
}
.login-card {
  width: 480px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.22);
  background: rgba(255,255,255,0.86);
  backdrop-filter: blur(14px);
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.35);
}
.card-header {
  text-align: center;
}
.card-header h2 {
  margin: 0 0 4px;
  color: #303133;
}
.card-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
.link-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
</style>
