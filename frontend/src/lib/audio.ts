export const playSound = (type: "confirm" | "forge" | "abdicate") => {
  if (typeof window === "undefined") return;
  
  const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
  if (!AudioContext) return;

  const context = new AudioContext();
  const oscillator = context.createOscillator();
  const gainNode = context.createGain();

  oscillator.connect(gainNode);
  gainNode.connect(context.destination);

  switch (type) {
    case "confirm":
      oscillator.type = "sine";
      oscillator.frequency.setValueAtTime(880, context.currentTime);
      gainNode.gain.setValueAtTime(0.2, context.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, context.currentTime + 0.1);
      oscillator.start();
      oscillator.stop(context.currentTime + 0.1);
      break;

    case "forge":
      oscillator.type = "sine";
      oscillator.frequency.setValueAtTime(80, context.currentTime);
      gainNode.gain.setValueAtTime(0.7, context.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, context.currentTime + 0.8);
      oscillator.start();
      oscillator.stop(context.currentTime + 0.8);
      break;

    case "abdicate":
      oscillator.type = "sawtooth";
      oscillator.frequency.setValueAtTime(220, context.currentTime);
      oscillator.frequency.linearRampToValueAtTime(110, context.currentTime + 0.3);
      gainNode.gain.setValueAtTime(0.5, context.currentTime);
      gainNode.gain.linearRampToValueAtTime(0.01, context.currentTime + 0.5);
      oscillator.start();
      oscillator.stop(context.currentTime + 0.5);
      break;
  }
};
