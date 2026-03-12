package org.firstinspires.ftc.teamcode.opmodes;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SelectCommand;
import com.seattlesolvers.solverslib.command.button.Trigger;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.subsystems.ClimbSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensorsSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.GateSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.LEDSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

import java.util.List;
@Config
@TeleOp(name = "Outreach Teleop", group = "!")
public class OutreachTeleOp extends CommandOpMode {
    //states
    private enum ShootState {
        SHOOTING, IDLE, IDK
    }
    public enum IntakeState {
        INTAKESTILL_ROLLERSIN, INTAKEOUT_ROLLERSOUT, INTAKEIN_ROLLERSIN, INTAKEOUT_ROLLERSIN, INTAKESTILL_ROLLERSSTILL
    }
    ShootState shootState = ShootState.IDLE;
    IntakeState intakeState = IntakeState.INTAKESTILL_ROLLERSIN;

    //subsystems
    private IntakeSubsystem intake;
    private ShooterSubsystem shooter;
    private SpindexerSubsystem spindexer;
    private ColorSensorsSubsystem colorSensors;
    private LEDSubsystem led;
    private GateSubsystem gate;
    private ClimbSubsystem climb;
    public GamepadEx driver1;
    public GamepadEx driver2;
    private Follower follower;

    List<LynxModule> allHubs;
    private final ElapsedTime totalTimer = new ElapsedTime();

    @Override
    public void initialize() {
        //Bulk reading
        allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
        intake = new IntakeSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap);
        colorSensors = new ColorSensorsSubsystem(hardwareMap);
        led = new LEDSubsystem(hardwareMap);
        gate = new GateSubsystem(hardwareMap);
        climb = new ClimbSubsystem(hardwareMap);
        driver1 = new GamepadEx(gamepad1);
        driver2 = new GamepadEx(gamepad2);
        follower = Constants.createFollower(hardwareMap);
        register(intake, shooter, spindexer, colorSensors, led, gate, climb);
        follower.startTeleopDrive();

        if (Math.abs(spindexer.getWrappedPosition() - 115) < 60) {
            spindexer.set(115);
        }
        else if (Math.abs(spindexer.getWrappedPosition() - 235) < 60){
            spindexer.set(235);
        }
        else if (Math.abs(spindexer.getWrappedPosition() - 355) < 60) {
            spindexer.set(355);
        }
        else {
            spindexer.set(115);
        }
        gate.up();

        createBinds();
    }

    public void run() {
        handleTeleOpDrive();
        handleShootState();
        handleLED();
        follower.update();
        super.run();
        //LEAVE THIS AT THE END

        for (LynxModule hub : allHubs) {
            hub.clearBulkCache();
        }
    }

    public void createBinds() {
        //driver 1
        driver1.getGamepadButton(GamepadKeys.Button.TRIANGLE).whenPressed(
                new InstantCommand(() -> {
                    if (intakeState == IntakeState.INTAKEIN_ROLLERSIN) intakeState = IntakeState.INTAKESTILL_ROLLERSIN;
                    else intakeState = IntakeState.INTAKEIN_ROLLERSIN;
                    new SelectCommand(this::getIntakeCommand).schedule();
                })
        );
        driver1.getGamepadButton(GamepadKeys.Button.CIRCLE).whenPressed(
                new InstantCommand(() -> {
                    spindexer.moveSpindexerBy(120);
                })
        );
        driver1.getGamepadButton(GamepadKeys.Button.SQUARE).whenPressed(
                new InstantCommand(() -> {
                    if (intakeState == IntakeState.INTAKEOUT_ROLLERSOUT) intakeState = IntakeState.INTAKESTILL_ROLLERSIN;
                    else {
                        intakeState = IntakeState.INTAKEOUT_ROLLERSOUT;
                        spindexer.moveSpindexerBy(-360);
                    }
                    new SelectCommand(this::getIntakeCommand).schedule();
                })
        );
        new Trigger(() -> driver1.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) > 0.5)
                .whenActive(new InstantCommand(() -> {
                    if (shootState == ShootState.SHOOTING) {
                        spindexer.moveSpindexerBy(360);
                    }
                }));

        //changes shooting state
        driver2.getGamepadButton(GamepadKeys.Button.TRIANGLE).whenPressed(
                new InstantCommand(() -> {
                    shootState = ShootState.IDLE;
                })
        );
        driver2.getGamepadButton(GamepadKeys.Button.CROSS).whenPressed(
                new InstantCommand(() -> {
                    shootState = ShootState.SHOOTING;
                })
        );
        driver2.getGamepadButton(GamepadKeys.Button.CIRCLE).whenPressed(
                new InstantCommand(() -> {
                    shootState = ShootState.IDK;
                })
        );
    }

    public void handleTeleOpDrive() {
        double y = driver1.getLeftY()*0.5;
        double x = driver1.getLeftX()*0.5;
        double rx = -driver1.getRightX()*0.5;
        double denominator = Math.max(Math.abs(x) + Math.abs(y) + Math.abs(rx), 1.0);

        follower.setTeleOpDrive(x/denominator, y/denominator, rx/denominator, true);
    }

    public void handleShootState() {
        switch(shootState) {
            case IDLE:
                shooter.setTargetLinearSpeed(0);
                gate.up();
                break;
            case SHOOTING:
                shooter.setTargetLinearSpeed(505);
                gate.down();
                break;
            case IDK:
                shooter.setTargetLinearSpeed(99999);
                gate.down();
                break;

        }
    }

    void handleLED() {
        //LED Code
        double t = totalTimer.seconds();
        double min = 0.3;
        double max = 0.722;
        double amplitude = (max - min) / 2.0;
        double midpoint = (max + min) / 2.0;
        double speed = 0.6; // cycles per second — increase for faster transitions

        // Oscillate servo position smoothly with sine wave
        double position = midpoint + amplitude * Math.sin(2 * Math.PI * speed * t);
        led.setPosition(position);
        led.setBlinkinLights(RevBlinkinLedDriver.BlinkinPattern.TWINKLES_RAINBOW_PALETTE);
    }

    public Command getIntakeCommand() {
        switch (intakeState) {
            case INTAKEIN_ROLLERSIN:
                return new InstantCommand(() -> {
                    intake.set(IntakeSubsystem.IntakeState.INTAKEIN_ROLLERSIN);
                });
            case INTAKEOUT_ROLLERSOUT:
                return new InstantCommand(() -> {
                    intake.set(IntakeSubsystem.IntakeState.INTAKEOUT_ROLLERSOUT);
                });
            case INTAKEOUT_ROLLERSIN:
                return new InstantCommand(() -> {
                    intake.set(IntakeSubsystem.IntakeState.INTAKEOUT_ROLLERSIN);
                });
            case INTAKESTILL_ROLLERSIN:
            default:
                return new InstantCommand(() -> {
                    intake.set(IntakeSubsystem.IntakeState.INTAKESTILL_ROLLERSIN);
                });
        }
    }

}
