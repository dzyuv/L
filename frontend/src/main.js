import { createApp } from "vue";
import axios from "axios";
import App from "./App.vue";
import { installChineseErrorMessages } from "./errorMessages";
import "./style.css";

installChineseErrorMessages(axios);
createApp(App).mount("#app");
