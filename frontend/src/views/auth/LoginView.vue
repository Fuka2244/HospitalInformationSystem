<template>
  <div class="login-container">
    <header class="login-topbar">
      <div class="topbar-brand">医院信息系统</div>
    </header>

    <el-card class="login-card" shadow="never">
      <div class="login-cap">
        <h2>{{ isLogin ? '用户登录' : '用户注册' }}</h2>
        <p>{{ isLogin ? '欢迎回来，登录后继续使用医疗服务' : '请完善信息完成账号注册' }}</p>
      </div>

      <div class="card-content">
        <!-- 登录表单 -->
        <el-form v-if="isLogin" class="login-form" ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="loginForm.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button class="submit-btn" type="primary" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form-item>
          <el-form-item>
            <div class="link-row">
              <el-link type="primary" @click="isLogin = false">没有账号？去注册</el-link>
              <el-link type="warning" @click="showForget = true">忘记密码？</el-link>
            </div>
          </el-form-item>
        </el-form>

        <!-- 注册表单 -->
        <el-form v-else class="register-form" ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="88px">
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
            <el-button class="submit-btn" type="primary" :loading="loading" @click="handleRegister">注 册</el-button>
          </el-form-item>
          <el-form-item>
            <el-link type="primary" @click="isLogin = true">已有账号？去登录</el-link>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <footer class="login-footer">© 2026 HIS · 医院信息系统</footer>

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
import { ref, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { forgetPassword, sendVerificationCode } from '@/api/patient'
import type { LoginDto, RegisterDto, ForgetPasswordDto } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const isLogin = ref(true)
const showForget = ref(false)
const codeLoading = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

watch(
  () => route.query.mode,
  (mode) => {
    isLogin.value = mode !== 'register'
  },
  { immediate: true },
)
  
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
  padding: 84px 16px 56px;
  background:
    radial-gradient(1100px 560px at 12% -8%, rgba(var(--his-primary-rgb), 0.16), transparent 62%),
    radial-gradient(980px 500px at 92% 6%, rgba(var(--his-primary-rgb), 0.10), transparent 68%),
    radial-gradient(640px 320px at 52% 100%, rgba(var(--his-primary-rgb), 0.08), transparent 72%),
    linear-gradient(180deg, var(--his-bg) 0%, var(--his-bg-2) 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before,
.login-container::after {
  content: "";
  position: absolute;
  width: 560px;
  height: 560px;
  border-radius: 50%;
  opacity: 0.2;
  pointer-events: none;
  filter: blur(20px);
}

.login-container::before {
  left: -190px;
  top: -220px;
  background: radial-gradient(circle at 35% 35%, rgba(var(--his-primary-rgb), 0.58), rgba(var(--his-primary-rgb), 0) 66%);
}

.login-container::after {
  right: -220px;
  bottom: -240px;
  background: radial-gradient(circle at 30% 30%, rgba(var(--his-primary-rgb), 0.44), rgba(var(--his-primary-rgb), 0) 66%);
}

.login-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 62px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 2;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0));
}

.topbar-brand {
  font-size: 16px;
  font-weight: 700;
  color: var(--his-text);
}

.topbar-links {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--his-primary-soft);
  color: var(--his-primary);
  border: 1px solid rgba(var(--his-primary-rgb), 0.22);
  font-size: 12px;
  font-weight: 600;
}

.login-card {
  width: min(408px, 92vw);
  border-radius: 20px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  background: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 255, 255, 0.8) 100%
  );
  backdrop-filter: saturate(175%) blur(20px);
  box-shadow:
    0 26px 60px rgba(18, 56, 38, 0.1),
    0 12px 24px rgba(var(--his-primary-rgb), 0.14);
  position: relative;
  padding-top: 58px;
  overflow: visible;
}

.login-card::before {
  content: "";
  position: absolute;
  inset: -1px;
  border-radius: 20px;
  pointer-events: none;
  background: linear-gradient(
    145deg,
    rgba(255, 255, 255, 0.58) 0%,
    rgba(255, 255, 255, 0.08) 30%,
    rgba(255, 255, 255, 0.02) 100%
  );
}

.login-cap {
  position: absolute;
  left: 16px;
  right: 16px;
  top: 0;
  transform: translateY(-36%);
  min-height: 82px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--his-primary), var(--his-accent));
  color: #fff;
  box-shadow: 0 14px 30px rgba(var(--his-primary-rgb), 0.30);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.login-cap h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.1;
  letter-spacing: 0.3px;
}

.login-cap p {
  margin: 8px 0 0;
  font-size: 11px;
  opacity: 0.9;
}

.card-content {
  padding: 14px 20px 18px;
  position: relative;
  z-index: 1;
  max-height: calc(100vh - 240px);
  overflow: auto;
}

.login-form :deep(.el-form-item),
.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item__label) {
  display: none;
}

.login-form :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.login-form :deep(.el-input__wrapper),
.register-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(18, 56, 38, 0.14) inset;
  background: rgba(255, 255, 255, 0.84);
  transition: box-shadow 0.2s ease, background-color 0.2s ease;
}

.login-form :deep(.el-input__wrapper.is-focus),
.register-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 1.5px rgba(var(--his-primary-rgb), 0.45) inset;
}

.login-form :deep(.el-input__wrapper) {
  height: 46px;
}

.register-form :deep(.el-input-number) {
  width: 100%;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  border: none;
  font-weight: 700;
  background: linear-gradient(135deg, var(--his-primary), var(--his-accent));
  box-shadow: 0 10px 22px rgba(var(--his-primary-rgb), 0.30);
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}

.submit-btn:hover {
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(var(--his-primary-rgb), 0.34);
}

.link-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 12px;
  text-align: center;
  color: var(--his-text-2);
  font-size: 12px;
  z-index: 2;
}

@media (max-width: 768px) {
  .login-topbar {
    padding: 0 14px;
  }

  .topbar-links {
    display: none;
  }

  .login-cap h2 {
    font-size: 24px;
  }

  .login-cap p {
    font-size: 12px;
  }
}
</style>
