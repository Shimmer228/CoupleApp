import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import path from "path";
import authRoutes from "./routes/auth.routes";
import pairRoutes from "./routes/pair.routes";
import taskRoutes from "./routes/task.routes";
import eventRoutes from "./routes/event.routes";
import transactionRoutes from "./routes/transaction.routes";
import wishlistRoutes from "./routes/wishlist.routes";
import profileRoutes from "./routes/profile.routes";
import rewardRoutes from "./routes/reward.routes";
import blueprintRoutes from "./routes/blueprint.routes";
import notificationRoutes from "./routes/notification.routes";
import { ensureAvatarUploadDir } from "./utils/avatar-storage";

dotenv.config();

const app = express();
ensureAvatarUploadDir();

app.use(cors());
app.use(express.json());
app.use("/uploads", express.static(path.join(process.cwd(), "uploads")));
app.use("/api/auth", authRoutes);
app.use("/api/pair", pairRoutes);
app.use("/api/tasks", taskRoutes);
app.use("/api/events", eventRoutes);
app.use("/api/transactions", transactionRoutes);
app.use("/api/wishlist", wishlistRoutes);
app.use("/api/profile", profileRoutes);
app.use("/api/rewards", rewardRoutes);
app.use("/api/blueprints", blueprintRoutes);
app.use("/api/notifications", notificationRoutes);

app.get("/", (req, res) => {
  res.send("API is working");
});

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
