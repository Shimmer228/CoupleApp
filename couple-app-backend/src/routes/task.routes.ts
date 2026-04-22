import { Router } from "express";
import { authenticate } from "../middleware/auth.middleware";
import {
  completeTask,
  createTask,
  failTask,
  getTasks,
  returnTask,
} from "../controllers/task.controller";

const router = Router();

router.use(authenticate);

router.get("/", getTasks);
router.post("/create", createTask);
router.post("/complete/:id", completeTask);
router.post("/return/:id", returnTask);
router.post("/fail/:id", failTask);

export default router;
