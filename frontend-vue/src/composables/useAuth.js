import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { login, registerAdmin, registerCompany } from '../api/auth';
import { useNotification } from './useNotification';

export function useAuth() {
  const router = useRouter();
  const { showSuccess, showError } = useNotification();
  
  const isAuthenticated = ref(false);
  const userRole = ref('');
  const userId = ref('');
  const loading = ref(false);
  
  // 从localStorage获取token
  const getToken = (role) => {
    return localStorage.getItem(`${role}_token`);
  };
  
  // 存储token到localStorage
  const setToken = (role, token) => {
    localStorage.setItem(`${role}_token`, token);
  };
  
  // 清除token
  const clearToken = (role) => {
    localStorage.removeItem(`${role}_token`);
  };
  
  // 初始化认证状态
  const initAuth = () => {
    // 检查是否存在token
    const adminToken = getToken('admin');
    const companyToken = getToken('company');
    
    if (adminToken) {
      isAuthenticated.value = true;
      userRole.value = 'admin';
    } else if (companyToken) {
      isAuthenticated.value = true;
      userRole.value = 'company';
    }
  };
  
  // 登录
  const handleLogin = async (username, password, role) => {
    try {
      loading.value = true;
      const response = await login(username, password);
      if (response.code === 200 && response.data) {
        const token = response.data;
        setToken(role, token);
        isAuthenticated.value = true;
        userRole.value = role;
        showSuccess('登录成功');
        
        // 跳转到对应角色的首页
        if (role === 'admin') {
          router.push('/admin/dashboard');
        } else if (role === 'company') {
          router.push('/company/dashboard');
        }
        
        return true;
      } else {
        showError(response.msg || '登录失败');
        return false;
      }
    } catch (error) {
      showError('登录失败，请稍后重试');
      return false;
    } finally {
      loading.value = false;
    }
  };
  
  // 注册
  const handleRegister = async (userData, role) => {
    try {
      loading.value = true;
      let response;
      if (role === 'admin') {
        response = await registerAdmin(userData);
      } else if (role === 'company') {
        response = await registerCompany(userData);
      } else {
        showError('无效的角色');
        return false;
      }
      if (response.code === 200) {
        showSuccess('注册成功');
        return true;
      } else {
        showError(response.msg || '注册失败');
        return false;
      }
    } catch (error) {
      showError('注册失败，请稍后重试');
      return false;
    } finally {
      loading.value = false;
    }
  };
  
  // 登出
  const handleLogout = (role) => {
    clearToken(role);
    isAuthenticated.value = false;
    userRole.value = '';
    showSuccess('登出成功');
    
    // 跳转到登录页
    if (role === 'admin') {
      router.push('/admin/login');
    } else if (role === 'company') {
      router.push('/company/login');
    }
  };
  
  return {
    isAuthenticated,
    userRole,
    userId,
    loading,
    getToken,
    setToken,
    clearToken,
    initAuth,
    handleLogin,
    handleRegister,
    handleLogout
  };
}