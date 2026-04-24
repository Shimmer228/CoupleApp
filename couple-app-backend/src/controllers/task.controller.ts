import { Response } from "express";
import { Prisma, TaskStatus } from "@prisma/client";
import { prisma } from "../config/prisma";
import { AuthenticatedRequest } from "../types/auth-request";
import { parseOptionalDate } from "../utils/date";

export const taskDetailsInclude = {
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
  completionRequestedBy: {
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

const taskContextSelect = {
  id: true,
  title: true,
  bank: true,
  status: true,
  pairId: true,
  assignedToId: true,
  createdById: true,
  completionRequestedById: true,
} satisfies Prisma.TaskSelect;

type TaskCreationInput = {
  userId: string;
  title: string;
  points: number;
  dueDate: Date | null;
};

type PairUser = {
  id: string;
  email: string;
};

const getUserContext = async (tx: Prisma.TransactionClient, userId: string) => {
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

  return user;
};

const getPairUsers = async (tx: Prisma.TransactionClient, pairId: string) => {
  const pairUsers = await tx.user.findMany({
    where: { pairId },
    select: {
      id: true,
      email: true,
    },
  });

  if (pairUsers.length !== 2) {
    throw new Error("PARTNER_REQUIRED");
  }

  return pairUsers;
};

const getTaskForPair = async (tx: Prisma.TransactionClient, taskId: string, pairId: string) => {
  const task = await tx.task.findUnique({
    where: { id: taskId },
    select: taskContextSelect,
  });

  if (!task || task.pairId !== pairId) {
    throw new Error("TASK_NOT_FOUND");
  }

  return task;
};

const getPartner = (pairUsers: PairUser[], userId: string) => {
  const partner = pairUsers.find((pairUser) => pairUser.id !== userId);

  if (!partner) {
    throw new Error("PARTNER_REQUIRED");
  }

  return partner;
};

const ensureAssignedUserCanAct = (task: Prisma.TaskGetPayload<{ select: typeof taskContextSelect }>, userId: string) => {
  if (task.status !== TaskStatus.ACTIVE) {
    throw new Error("TASK_NOT_ACTIVE");
  }

  if (task.assignedToId !== userId) {
    throw new Error("FORBIDDEN_ACTION");
  }
};

const ensureCreatorCanModify = (task: Prisma.TaskGetPayload<{ select: typeof taskContextSelect }>, userId: string) => {
  if (task.createdById !== userId) {
    throw new Error("FORBIDDEN_CREATOR_ACTION");
  }

  if (task.status !== TaskStatus.ACTIVE) {
    throw new Error("TASK_NOT_EDITABLE");
  }
};

export const createTaskForUser = async (tx: Prisma.TransactionClient, input: TaskCreationInput) => {
  const { userId, title, points, dueDate } = input;
  const user = await getUserContext(tx, userId);
  const pairUsers = await getPairUsers(tx, user.pairId!);

  if (user.points < points) {
    throw new Error("INSUFFICIENT_POINTS");
  }

  const partner = getPartner(pairUsers, userId);

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
      completionRequestedById: null,
      pairId: user.pairId!,
      dueDate,
    },
    include: taskDetailsInclude,
  });

  return {
    task,
    currentUserPoints: updatedUser.points,
  };
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
      return res.status(400).json({ message: "Only active tasks can be changed this way" });
    }

    if (error.message === "TASK_NOT_EDITABLE") {
      return res.status(400).json({ message: "Only active tasks can be edited or deleted" });
    }

    if (error.message === "WAITING_CONFIRMATION_REQUIRED") {
      return res.status(400).json({ message: "Task must be waiting for confirmation" });
    }

    if (error.message === "COMPLETION_REQUEST_REQUIRED") {
      return res.status(400).json({ message: "Task does not have a pending completion request" });
    }

    if (error.message === "FORBIDDEN_ACTION") {
      return res.status(403).json({ message: "Only the assigned user can respond to this task" });
    }

    if (error.message === "FORBIDDEN_CREATOR_ACTION") {
      return res.status(403).json({ message: "Only the task creator can edit or delete this task" });
    }

    if (error.message === "FORBIDDEN_CONFIRMATION") {
      return res.status(403).json({ message: "Only the other partner can confirm or reject completion" });
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

    if (error.message === "TASK_UPDATE_REQUIRED") {
      return res.status(400).json({ message: "Provide a title or due date to update" });
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

    const result = await prisma.$transaction((tx) =>
      createTaskForUser(tx, {
        userId,
        title,
        points,
        dueDate,
      })
    );

    return res.status(201).json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const updateTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");
    const hasTitle = Object.prototype.hasOwnProperty.call(req.body, "title");
    const hasDueDate = Object.prototype.hasOwnProperty.call(req.body, "dueDate");
    const title = hasTitle ? normalizeTitle(String(req.body.title ?? "")) : undefined;
    const dueDate = hasDueDate ? parseOptionalDate(req.body.dueDate) : undefined;

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    if (!hasTitle && !hasDueDate) {
      throw new Error("TASK_UPDATE_REQUIRED");
    }

    if (hasTitle && !title) {
      return res.status(400).json({ message: "Task title is required" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await getUserContext(tx, userId);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      ensureCreatorCanModify(task, userId);

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          ...(hasTitle ? { title } : {}),
          ...(hasDueDate ? { dueDate: dueDate ?? null } : {}),
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: user.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const deleteTask = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await getUserContext(tx, userId);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      ensureCreatorCanModify(task, userId);

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

      await tx.task.delete({
        where: { id: task.id },
      });

      return {
        deletedId: task.id,
        currentUserPoints: updatedUser.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const requestCompletion = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await getUserContext(tx, userId);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      ensureAssignedUserCanAct(task, userId);

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.WAITING_CONFIRMATION,
          completionRequestedById: userId,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: user.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const confirmCompletion = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await getUserContext(tx, userId);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      if (task.status !== TaskStatus.WAITING_CONFIRMATION) {
        throw new Error("WAITING_CONFIRMATION_REQUIRED");
      }

      if (!task.completionRequestedById) {
        throw new Error("COMPLETION_REQUEST_REQUIRED");
      }

      if (task.completionRequestedById === userId || task.assignedToId === userId) {
        throw new Error("FORBIDDEN_CONFIRMATION");
      }

      await getPairUsers(tx, user.pairId!);

      await tx.user.update({
        where: { id: task.completionRequestedById },
        data: {
          points: {
            increment: task.bank,
          },
          winStreak: {
            increment: 1,
          },
        },
      });

      await tx.user.update({
        where: { id: userId },
        data: {
          winStreak: 0,
        },
      });

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.COMPLETED,
          completionRequestedById: null,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: user.points,
      };
    });

    return res.json(result);
  } catch (error) {
    return sendTaskError(res, error);
  }
};

export const rejectCompletion = async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userId = req.userId;
    const taskId = String(req.params.id ?? "");

    if (!userId) {
      return res.status(401).json({ message: "Unauthorized" });
    }

    const result = await prisma.$transaction(async (tx) => {
      const user = await getUserContext(tx, userId);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      if (task.status !== TaskStatus.WAITING_CONFIRMATION) {
        throw new Error("WAITING_CONFIRMATION_REQUIRED");
      }

      if (!task.completionRequestedById) {
        throw new Error("COMPLETION_REQUEST_REQUIRED");
      }

      if (task.completionRequestedById === userId || task.assignedToId === userId) {
        throw new Error("FORBIDDEN_CONFIRMATION");
      }

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.ACTIVE,
          completionRequestedById: null,
        },
        include: taskDetailsInclude,
      });

      return {
        task: updatedTask,
        currentUserPoints: user.points,
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
      const user = await getUserContext(tx, userId);
      const pairUsers = await getPairUsers(tx, user.pairId!);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      ensureAssignedUserCanAct(task, userId);

      if (user.points < task.bank) {
        throw new Error("INSUFFICIENT_POINTS");
      }

      const partner = getPartner(pairUsers, userId);
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
          assignedToId: partner.id,
          completionRequestedById: null,
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
      const user = await getUserContext(tx, userId);
      const pairUsers = await getPairUsers(tx, user.pairId!);
      const task = await getTaskForPair(tx, taskId, user.pairId!);

      ensureAssignedUserCanAct(task, userId);

      if (user.points < task.bank) {
        throw new Error("INSUFFICIENT_POINTS");
      }

      const partner = getPartner(pairUsers, userId);
      const updatedUser = await tx.user.update({
        where: { id: userId },
        data: {
          points: {
            decrement: task.bank,
          },
          winStreak: 0,
        },
        select: {
          points: true,
        },
      });

      await tx.user.update({
        where: { id: partner.id },
        data: {
          points: {
            increment: task.bank,
          },
          winStreak: {
            increment: 1,
          },
        },
      });

      const updatedTask = await tx.task.update({
        where: { id: task.id },
        data: {
          status: TaskStatus.FAILED,
          completionRequestedById: null,
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
