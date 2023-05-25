import axios from "axios";

const SECOND = 1000;

const ajax = axios.create({
  baseURL: import.meta.env.VITE_API || location.origin,
  timeout: 120 * SECOND,
});

export default ajax;
