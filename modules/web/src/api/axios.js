import axios from "axios";

const SECOND = 1000;
const remoteIp = ref(localStorage.getItem("remoteIp"));

const ajax = axios.create({
  // baseURL: import.meta.env.VITE_API || location.origin,
  timeout: 120 * SECOND,
});

ajax.interceptors.request.use((config) => {
  config.baseURL = baseUrl();
  return config;
});

export default ajax;

export const setRemoteIp = (ip) => {
  remoteIp.value = ip;
  localStorage.setItem("remoteIp", ip);
};

export const baseUrl = () => {
  return remoteIp.value || location.origin;
};
