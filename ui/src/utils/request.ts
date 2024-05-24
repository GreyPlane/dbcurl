import axios from "axios";

export const apiRequest = axios.create({
  baseURL: "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json; charset=UTF-8",
  }
})
