import { Router } from "express";
import { authenticate } from "../middleware/auth.middleware";
import {
  createTransaction,
  deleteTransaction,
  getTransactionBalance,
  getTransactionSummary,
  getTransactions,
  updateTransaction,
} from "../controllers/transaction.controller";

const router = Router();

router.use(authenticate);

router.post("/create", createTransaction);
router.put("/:id", updateTransaction);
router.delete("/:id", deleteTransaction);
router.get("/", getTransactions);
router.get("/balance", getTransactionBalance);
router.get("/summary", getTransactionSummary);

export default router;
