package org.example;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Toolkit;

public class PomodoroTimer {

    // Default Pomodoro settings in seconds
    private static int WORK_DURATION = 25 * 60;          // 25 minutes
    private static int SHORT_BREAK = 5 * 60;             // 5 minutes
    private static int LONG_BREAK = 15 * 60;             // 15 minutes
    private static final int CYCLES_BEFORE_LONG_BREAK = 4; // After 4 work sessions

    private static int cycleCount = 0;                    // Total cycles completed
    private static int workSessionsCompleted = 0;         // Total work sessions completed
    private static boolean isRunning = false;             // Timer running state
    private static TimerThread timerThread = null;        // Reference to the timer thread
    private static final String PROGRESS_FILE = "pomodoro_progress.txt"; // Progress file

    public static void main(String[] args) {
        loadProgress(); // Load existing progress if available
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Enhanced Pomodoro Timer!");

        while (true) {
            printMenu(); // Display menu options
            String input = scanner.nextLine().trim().toLowerCase(); // Get user input

            switch (input) {
                case "s":
                    if (!isRunning) {
                        startTimer(scanner);
                    } else {
                        System.out.println("Timer is already running.");
                    }
                    break;
                case "p":
                    pauseTimer();
                    break;
                case "r":
                    resumeTimer();
                    break;
                case "c":
                    resetTimer();
                    break;
                case "set":
                    setDurations(scanner);
                    break;
                case "v":
                    viewProgress();
                    break;
                case "q":
                    exitApplication(scanner);
                    break;
                default:
                    System.out.println("Invalid command. Please try again.");
            }
        }
    }

    // Displays the menu options to the user
    private static void printMenu() {
        System.out.println("\nPlease choose an option:");
        System.out.println("s - Start Timer");
        System.out.println("p - Pause Timer");
        System.out.println("r - Resume Timer");
        System.out.println("c - Reset Timer");
        System.out.println("set - Set Durations");
        System.out.println("v - View Progress");
        System.out.println("q - Quit");
        System.out.print("Enter your choice: ");
    }

    // Starts the timer by initializing and starting the TimerThread
    private static void startTimer(Scanner scanner) {
        System.out.println("Starting Pomodoro Timer...");
        isRunning = true;
        timerThread = new TimerThread();
        timerThread.start();
    }

    // Pauses the timer by invoking pause on the TimerThread
    private static void pauseTimer() {
        if (isRunning && timerThread != null) {
            timerThread.pauseTimer();
            System.out.println("Timer paused.");
        } else {
            System.out.println("No active timer to pause.");
        }
    }

    // Resumes the timer by invoking resume on the TimerThread
    private static void resumeTimer() {
        if (isRunning && timerThread != null) {
            timerThread.resumeTimer();
            System.out.println("Timer resumed.");
        } else {
            System.out.println("No paused timer to resume.");
        }
    }

    // Resets the timer and progress counters
    private static void resetTimer() {
        if (timerThread != null) {
            timerThread.interrupt(); // Interrupt the timer thread
            timerThread = null;
        }
        isRunning = false;
        cycleCount = 0;
        workSessionsCompleted = 0;
        System.out.println("Timer reset.");
    }

    // Allows the user to set custom durations for sessions
    private static void setDurations(Scanner scanner) {
        try {
            System.out.print("Enter work duration in minutes (current: " + (WORK_DURATION / 60) + "): ");
            String workInput = scanner.nextLine().trim();
            if (!workInput.isEmpty()) {
                int work = Integer.parseInt(workInput);
                if (work > 0) {
                    WORK_DURATION = work * 60;
                } else {
                    System.out.println("Invalid work duration. Keeping previous value.");
                }
            }

            System.out.print("Enter short break duration in minutes (current: " + (SHORT_BREAK / 60) + "): ");
            String shortBreakInput = scanner.nextLine().trim();
            if (!shortBreakInput.isEmpty()) {
                int shortB = Integer.parseInt(shortBreakInput);
                if (shortB > 0) {
                    SHORT_BREAK = shortB * 60;
                } else {
                    System.out.println("Invalid short break duration. Keeping previous value.");
                }
            }

            System.out.print("Enter long break duration in minutes (current: " + (LONG_BREAK / 60) + "): ");
            String longBreakInput = scanner.nextLine().trim();
            if (!longBreakInput.isEmpty()) {
                int longB = Integer.parseInt(longBreakInput);
                if (longB > 0) {
                    LONG_BREAK = longB * 60;
                } else {
                    System.out.println("Invalid long break duration. Keeping previous value.");
                }
            }

            System.out.println("Durations updated successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter numeric values.");
        }
    }

    // Displays the user's progress
    private static void viewProgress() {
        System.out.println("\n--- Pomodoro Progress ---");
        System.out.println("Work Sessions Completed: " + workSessionsCompleted);
        System.out.println("Total Cycles Completed: " + cycleCount);
        System.out.println("-------------------------");
    }

    // Exits the application after saving progress
    private static void exitApplication(Scanner scanner) {
        System.out.println("Exiting Pomodoro Timer. Goodbye!");
        saveProgress(); // Save progress before exiting
        scanner.close();
        System.exit(0);
    }

    // Loads progress from the progress file
    private static void loadProgress() {
        File file = new File(PROGRESS_FILE);
        if (file.exists()) {
            try (Scanner fileScanner = new Scanner(file)) {
                if (fileScanner.hasNextInt()) {
                    workSessionsCompleted = fileScanner.nextInt();
                }
                if (fileScanner.hasNextInt()) {
                    cycleCount = fileScanner.nextInt();
                }
            } catch (IOException e) {
                System.out.println("Error reading progress file.");
            }
        }
    }

    // Saves progress to the progress file
    private static void saveProgress() {
        try (FileWriter writer = new FileWriter(PROGRESS_FILE)) {
            writer.write(workSessionsCompleted + "\n" + cycleCount);
        } catch (IOException e) {
            System.out.println("Error saving progress.");
        }
    }

    // Inner class to handle the timer in a separate thread
    private static class TimerThread extends Thread {
        private boolean paused = false;

        @Override
        public void run() {
            try {
                while (true) {
                    // Work Session
                    runSession("Work", WORK_DURATION);
                    workSessionsCompleted++;
                    saveProgress(); // Save after completing a work session

                    cycleCount++;
                    if (cycleCount % CYCLES_BEFORE_LONG_BREAK == 0) {
                        runSession("Long Break", LONG_BREAK);
                    } else {
                        runSession("Short Break", SHORT_BREAK);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Timer interrupted.");
            }
        }

        // Runs a single session (work or break)
        private void runSession(String sessionType, int durationInSeconds) throws InterruptedException {
            System.out.println("\nStarting " + sessionType + " session for " + (durationInSeconds / 60) + " minutes.");
            int remaining = durationInSeconds;

            while (remaining > 0) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                synchronized (this) {
                    while (paused) {
                        wait(); // Wait while paused
                    }
                }

                int minutes = remaining / 60;
                int seconds = remaining % 60;
                System.out.print(String.format("\rTime remaining: %02d:%02d", minutes, seconds));
                TimeUnit.SECONDS.sleep(1);
                remaining--;
            }

            System.out.println("\n" + sessionType + " session completed.");
            playSound(); // Play sound after session completion
        }

        // Pauses the timer
        public void pauseTimer() {
            paused = true;
        }

        // Resumes the timer
        public synchronized void resumeTimer() {
            paused = false;
            notify(); // Notify the thread to resume
        }

        // Plays a sound alert
        private void playSound() {
            try {
                // Simple beep sound
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                System.out.println("Error playing sound.");
            }
        }
    }
}
