import { Variants } from "framer-motion";

export const containerVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.15,
      delayChildren: 0.3,
    },
  },
  exit: {
    opacity: 0,
    transition: { duration: 0.5 },
  },
};

export const itemVariants: Variants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.7, ease: "easeInOut" },
  },
};

export const textRevealVariants: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.03,
      delayChildren: 0.5,
    },
  },
};

export const letterVariants: Variants = {
  hidden: { opacity: 0, y: 10 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.3, ease: "easeOut" },
  },
};

export const buttonHoverEffect = {
  scale: 1.02,
  boxShadow: "0 0 15px rgba(234,234,234,0.1)",
  transition: { duration: 0.3 },
};

export const buttonTapEffect = {
  scale: 0.98,
  transition: { duration: 0.1 },
};
