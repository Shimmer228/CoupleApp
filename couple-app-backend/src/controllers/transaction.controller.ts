import { Prisma, TransactionCategory, TransactionType } from "@prisma/client";
import { Response } from "express";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";

const transactionInclude = {
  createdBy: {
    select: {
      id: true,
      email: true,
    },
  },
} satisfies Prisma.TransactionInclude;

const transactionOrderBy = {
  createdAt: "desc",
} satisfies Prisma.TransactionOrderByWithRelationInput;

const normalizeTitle = (value: string) => value.trim();

const parseTransactionType = (value: unknown) => {
  const normalized = String(value ?? "").trim().toUpperCase();

  if (normalized === TransactionType.EXPENSE || normalized === TransactionType.INCOME) {
    return normalized;
  }

  throw new Error("INVALID_TRANSACTION_TYPE");
};

const parseTransactionCategory = (value: unknown) => {
  const normalized = String(value ?? "").trim().toUpperCase();

  if (
    normalized === TransactionCategory.SELF ||
    normalized === TransactionCategory.PARTNER ||
    normalized === TransactionCategory.SHARED
  ) {
    return normalized;
  }

  throw new Error("INVALID_TRANSACTION_CATEGORY");
};

const getPairUsers = async (userId: string) => {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: {
      id: true,
      pairId: true,
    },
  });

  if (!user) {
    throw new Error("USER_NOT_FOUND");
  }

  if (!user.pairId) {
    throw new Error("PAIR_REQUIRED");
  }

  const pairUsers = await prisma.user.findMany({
    where: { pairId: user.pairId },
    select: {
      id: true,
      email: true,
    },
  });

  if (pairUsers.length !== 2) {
    throw new Error("PAIR_MUST_HAVE_TWO_USERS");
  }

  const partner = pairUsers.find((pairUser) => pairUser.id !== user.id);

  if (!partner) {
    throw new Error("PAIR_MUST_HAVE_TWO_USERS");
  }

  return {
    user,
    partner,
    pairId: user.pairId,
  };
};

const getBalanceContribution = (
  transaction: {
    amount: number;
    type: TransactionType;
    category: TransactionCategory;
    createdById: string;
  },
  currentUserId: string
) => {
  const isCreator = transaction.createdById === currentUserId;

  if (transaction.category === TransactionCategory.SELF) {
    return 0;
  }

  if (transaction.type === TransactionType.EXPENSE && transaction.category === TransactionCategory.PARTNER) {
    return isCreator ? transaction.amount : -transaction.amount;
  }

  if (transaction.type === TransactionType.EXPENSE && transaction.category === TransactionCategory.SHARED) {
    return isCreator ? transaction.amount / 2 : -(transaction.amount / 2);
  }

  if (transaction.type === TransactionType.INCOME && transaction.category === TransactionCategory.PARTNER) {
    return isCreator ? -transaction.amount : transaction.amount;
  }

  if (transaction.type === TransactionType.INCOME && transaction.category === TransactionCategory.SHARED) {
    return isCreator ? -(transaction.amount / 2) : transaction.amount / 2;
  }

  return 0;
};

const sendTransactionError = (res: Response, error: unknown) => {
  if (error instanceof Error) {
    if (error.message === "USER_NOT_FOUND") {
      return res.status(404).json({ message: "User not found" });
    }

    if (error.message === "PAIR_REQUIRED") {
      return res.status(400).json({ message: "You need to be connected to a pair first" });
    }

    if (error.message === "PAIR_MUST_HAVE_TWO_USERS") {
      return res.status(400).json({ message: "Finance system requires exactly two paired users" });
    }

    if (error.message === "INVALID_TRANSACTION_TYPE") {
      return res.status(400).json({ message: "Invalid transaction type" });
    }

    if (error.message === "INVALID_TRANSACTION_CATEGORY") {
      return res.status(400).json({ message: "Invalid transaction category" });
    }
  }

  console.error("Transaction controller error:", error);
  return res.status(500).json({ message: "Internal server error" });
};

export const createTransaction = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const title = normalizeTitle(String(req.body.title ?? ""));
    const amount = Number(req.body.amount);

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!title) {
      return res.status(400).json({ message: "Transaction title is required" });
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      return res.status(400).json({ message: "Amount must be greater than 0" });
    }

    const type = parseTransactionType(req.body.type);
    const category = parseTransactionCategory(req.body.category);
    const pairContext = await getPairUsers(userId);

    const transaction = await prisma.transaction.create({
      data: {
        title,
        amount,
        type,
        category,
        createdById: userId,
        pairId: pairContext.pairId!,
      },
      include: transactionInclude,
    });

    return res.status(201).json({ transaction });
  } catch (error) {
    return sendTransactionError(res, error);
  }
};

export const getTransactions = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const pairContext = await getPairUsers(userId);

    const transactions = await prisma.transaction.findMany({
      where: { pairId: pairContext.pairId! },
      include: transactionInclude,
      orderBy: transactionOrderBy,
    });

    return res.json({
      currentUserId: userId,
      transactions,
    });
  } catch (error) {
    return sendTransactionError(res, error);
  }
};

export const getTransactionBalance = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const pairContext = await getPairUsers(userId);

    const transactions = await prisma.transaction.findMany({
      where: { pairId: pairContext.pairId! },
      select: {
        amount: true,
        type: true,
        category: true,
        createdById: true,
      },
    });

    const rawBalance = transactions.reduce((sum, transaction) => {
      return sum + getBalanceContribution(transaction, userId);
    }, 0);

    return res.json({
      balance: Number(Math.abs(rawBalance).toFixed(2)),
      direction: rawBalance < 0 ? "YOU_OWE" : "PARTNER_OWES",
    });
  } catch (error) {
    return sendTransactionError(res, error);
  }
};
