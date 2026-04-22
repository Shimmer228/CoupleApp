import { Response } from "express";
import { Prisma, TaskStatus } from "@prisma/client";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";
import { parseOptionalDate } from "../utils/date";

const taskDetailsInclude = {
  createdBy: {
    select: {
      id: true,
      email: true,
    },
  },
  assignedTo: {
    select: {
      id: true,
      email: true,
    },
  },
} satisfies Prisma.TaskInclude;

const taskOrderBy = {
  createdAt: "desc",
} satisfies Prisma.TaskOrderByWithRelationInput;

const normalizeTitle = (value: string) => value.trim();

const getActionableContext = async (tx: Prisma.TransactionClient, userId: string, taskId: string) => {
  const user = await tx.user.findUnique({
    where: { id: userId },
    select: {
      id: true,
      pairId: true,
      points: true,
    },
  });

  if (!user) {
    throw new Error("USER_NOT_FOUND");
  }

  if (!user.pairId) {
    throw new Error("PAIR_REQUIRED");
  }

  const task = await tx.task.findUnique({
    where: { id: taskId },
    select: {
      id: true,
      title: true,
      bank: true,
      status: true,
      pairId: true,
      assignedToId: true,
      createdById: true,
    },
  });

  if (!task || task.pairId !== user.pairId) {
    throw new Error("TASK_NOT_FOUND");
  }

  if (task.status !== TaskStatus.ACTIVE) {
    throw new Error("TASK_NOT_ACTIVE");
  }

  if (task.assignedToId !== userId) {
    throw new Error("FORBIDDEN_ACTION");
  }

  const pairUsers = await tx.user.findMany({
    where: { pairId: user.pairId },
    select: {
      id: true,
      email: true,
    },
  });

  const opponent = pairUsers.find((pairUser) => pairUser.id !== userId);

  if (!opponent) {
    throw new Error("PARTNER_REQUIRED");
  }

  return { user, task, opponent };
};

const sendTaskError = (res: Response, error: unknown) => {
  if (error instanceof Error) {
    if (error.message === "USER_NOT_FOUND") {
      return res.status(404).json({ message: "User not found" });
    }

    if (error.message === "PAIR_REQUIRED") {
      return res.status(400).json({ message: "You need to be connected to a pair first" });
    }

    if (error.message === "TASK_NOT_FOUND") {
      return res.status(404).json({ message: "Task not found" });
    }

    if (error.message === "TASK_NOT_ACTIVE") {
      return res.status(400).json({ message: "Only active tasks can be updated" });
    }

    if (error.message === "FORBIDDEN_ACTION") {
      return res.status(403).json({ message: "Only the assigned user can respond to this task" });
    }

    if (error.message === "PARTNER_REQUIRED") {
      return res.status(400).json({ message: "Your pair must contain two users" });
    }

    if (error.message === "INSUFFICIENT_POINTS") {
      return res.status(400).json({ message: "Not enough points for this action" });
    }

    if (error.message === "INVALID_DATE") {
      return res.status(400).json({ message: "Invalid due date format" });
    }
  }

  console.error("Task controller error:", error);
  return res.status(500).json({ message: "Internal server error" });
};

export const getTasks = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        pairId: true,
        points: true,
      },
    });

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    if (!user.pairId) {
      return res.status(400).json({ message: "You need to be connected to a pair first" });
    }

    const tasks = await prisma.task.findMany({
      where: { pairId: user.pairId },
      include: taskDetailsInclude,
      orderBy: taskOrderBy,
    });

    return res.json({
      currentUserId: user.id,
      currentUserPoints: user.points,
      tasks,
    });
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const createTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const title = normalizeTitle(String(req.body.title ?? ""));
    const points = Number(req.body.points);
    const dueDate = parseOptionalDate(req.body.dueDate);

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!title) {
      return res.status(400).json({ message: "Task title is required" });
    }

    if (!Number.isInteger(points) || points <= 0) {
      return res.status(400).json({ message: "Points must be a positive integer" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await tx.user.findUnique({
        where: { id: userId },
        select: {
          id: true,
          pairId: true,
          points: true,
        },
      });

      if (!user) {
        throw new Error("USER_NOT_FOUND");
      }

      if (!user.pairId) {
        throw new Error("PAIR_REQUIRED");
      }

      if (user.points < points) {
        throw new Error("INSUFFICIENT_POINTS");
      }

      const pairUsers = await tx.user.findMany({
        where: { pairId: user.pairId },
        select: {
          id: true,
          email: true,
        },
      });

      const partner = pairUsers.find((pairUser) => pairUser.id !== userId);

      if (!partner || pairUsers.length !== 2) {
        throw new Error("PARTNER_REQUIRED");
      }

      const updatedUser = await tx.user.update({
        where: { id: userId },
        data: {
          points: {
            decrement: points,
          },
        },
        select: {
          points: true,
        },
      });

      const task = await tx.task.create({
        data: {
          title,
          bank: points,
          status: TaskStatus.ACTIVE,
          assignedToId: partner.id,
          createdById: userId,
          pairId: user.pairId,
          dueDate,
        },
        include: taskDetailsInclude,
      });

      return {
        task,
        currentUserPoints: updatedUser.points,
      };
    });

    return res.status(201).json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const completeTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const { task } = await getActionableContext(tx, userId, taskId);

      const updatedUser = await tx.user.update({
        where: { id: userId },
        data: {
          points: {
            increment: task.bank,
          },
        },
        select: {
          points: true,
        },
      });

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.COMPLETED,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: updatedUser.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const returnTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const { user, task, opponent } = await getActionableContext(tx, userId, taskId);

      if (user.points < task.bank) {
        throw new Error("INSUFFICIENT_POINTS");
      }

      const updatedUser = await tx.user.update({
        where: { id: userId },
        data: {
          points: {
            decrement: task.bank,
          },
        },
        select: {
          points: true,
        },
      });

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          bank: {
            increment: task.bank,
          },
          assignedToId: opponent.id,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: updatedUser.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const failTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const { user, task, opponent } = await getActionableContext(tx, userId, taskId);

      if (user.points < task.bank) {
        throw new Error("INSUFFICIENT_POINTS");
      }

      const updatedUser = await tx.user.update({
        where: { id: userId },
        data: {
          points: {
            decrement: task.bank,
          },
        },
        select: {
          points: true,
        },
      });

      await tx.user.update({
        where: { id: opponent.id },
        data: {
          points: {
            increment: task.bank,
          },
        },
      });

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.FAILED,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: updatedUser.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};
