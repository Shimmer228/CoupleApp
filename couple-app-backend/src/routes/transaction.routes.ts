import { Router } from "express";
import { authenticate } from "../middleware/auth.middleware";
import {
  createTransaction,
  getTransactionBalance,
  getTransactions,
} from "../controllers/transaction.controller";

const router = Router();

router.use(authenticate);

router.post("/create", createTransaction);
router.get("/", getTransactions);
router.get("/balance", getTransactionBalance);

export default router;
