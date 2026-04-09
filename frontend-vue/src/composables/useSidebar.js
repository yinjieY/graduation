import { ref, provide, inject, onMounted, onUnmounted } from 'vue';

const sidebarStateSymbol = Symbol('sidebarState');

export function provideSidebarState(role) {
  const isCollapsed = ref(false);
  
  const toggleCollapse = () => {
    isCollapsed.value = !isCollapsed.value;
    localStorage.setItem(`sidebar_collapsed_${role}`, isCollapsed.value.toString());
  };
  
  const loadState = () => {
    const savedState = localStorage.getItem(`sidebar_collapsed_${role}`);
    if (savedState !== null) {
      isCollapsed.value = savedState === 'true';
    }
  };
  
  const handleStorageChange = (e) => {
    if (e.key === `sidebar_collapsed_${role}`) {
      isCollapsed.value = e.newValue === 'true';
    }
  };
  
  onMounted(() => {
    loadState();
    window.addEventListener('storage', handleStorageChange);
  });
  
  onUnmounted(() => {
    window.removeEventListener('storage', handleStorageChange);
  });
  
  provide(sidebarStateSymbol, {
    isCollapsed,
    toggleCollapse
  });
  
  return { isCollapsed, toggleCollapse };
}

export function useSidebarState() {
  const state = inject(sidebarStateSymbol);
  if (!state) {
    throw new Error('useSidebarState must be used within a component that provides sidebar state');
  }
  return state;
}