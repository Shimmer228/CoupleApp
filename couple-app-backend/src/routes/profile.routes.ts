import { Router } from "express";
import { authenticate } from "../middleware/auth.middleware";
import { getMyProfile, getPartnerProfile, updateMyProfile } from "../controllers/profile.controller";

const router = Router();

router.use(authenticate);

router.get("/me", getMyProfile);
router.put("/me", updateMyProfile);
router.get("/partner", getPartnerProfile);

export default router;
