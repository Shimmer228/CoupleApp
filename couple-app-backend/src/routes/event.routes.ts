import { Router } from "express";
import { authenticate } from "../middleware/auth.middleware";
import { createEvent, getAllEvents, getEventsForDate } from "../controllers/event.controller";

const router = Router();

router.use(authenticate);

router.post("/create", createEvent);
router.get("/all", getAllEvents);
router.get("/", getEventsForDate);

export default router;
