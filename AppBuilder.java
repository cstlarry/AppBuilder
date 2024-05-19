import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.control.Alert.AlertType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.util.*;

import static java.util.FormatProcessor.FMT;
/**
 * This class is intended to be an IDE Template that can be selected from the
 * IDE menu such that the user is prompted to change the class name fitting
 * the project.
 */
public class AppBuilder extends Application {

    private final BorderPane root = new BorderPane();

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 600;
    private static final int OUTPUT_FONT_SIZE = 16;
    private static final int DEFAULT_PADDING = 10;
    private static final String STYLE = STR."""
                -fx-font-family: monospace;
                -fx-font-weight: bold;
                -fx-font-size: \{OUTPUT_FONT_SIZE};
                """;
    private final HBox buttonBox = new HBox(DEFAULT_PADDING);
    private final VBox controls = new VBox(DEFAULT_PADDING);

    private final Button runBtn = new Button("Run");
    private final Button clearBtn = new Button("Clear Output");
    private final Button printBtn = new Button("Print");
    private final TextArea display = new TextArea();
    private final Label status = new Label("Status");
    private final StringBuilder output = new StringBuilder(128);
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final ClipboardContent content = new ClipboardContent();
    private final GridPane form = new GridPane();
    private final SecureRandom random = new SecureRandom();
    private final Label instructions = new Label();

    private final Button openBtn = new Button("Open");
    private final Button saveBtn = new Button("Save");
    private final FileChooser filer = new FileChooser();
    private final FileChooser.ExtensionFilter extFilter =
            new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
    private File file = null;
    private Stage stage = null;

    private final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance();

    private String appTitle;
    private Label[] labels;
    private TextField[] fields;
    private int rows;
    private int nextRow;

    /**
     * Constructs and returns a Parent node populated with UI components
     * for the application. The UI includes a TextArea for display, various
     * buttons for actions like running a process, clearing the output,
     * printing content, opening and saving files, and a ComboBox for selecting
     * options. Each component is styled and configured with event handlers
     * to perform its respective action.
     *
     * <p>The layout consists of:
     * <ul>
     *     <li>A top section with forms.</li>
     *     <li>A center section with a TextArea for display.</li>
     *     <li>A bottom section with a status bar.</li>
     *     <li>A button box containing all operational buttons.</li>
     * </ul>
     *
     * <p>Actions include:
     * <ul>
     *     <li>Running a process.</li>
     *     <li>Clearing the display output.</li>
     *     <li>Copying and saving the display text to a file.</li>
     *     <li>Opening a file with a FileChooser and displaying its content.</li>
     *     <li>Saving the current display content to a file.</li>
     *     <li>Updating the console based on the selected item in the ComboBox.</li>
     * </ul>
     *
     * @return the root node containing all the UI elements.
     */
    private Parent createContent() {
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setPadding(new Insets(DEFAULT_PADDING));
        //root.setStyle(STYLE);

        display.setPrefColumnCount(50);
        display.setWrapText(true);
        display.setEditable(true);
        display.setStyle(STYLE);
        runBtn.setOnAction(e -> {
            try {
                run();
            } catch (Exception ex) {
                println(ex);
            }
        });
        clearBtn.setOnAction(e -> clearOutput());
        printBtn.setOnAction(
                e -> {
                    content.putString(display.getText());
                    clipboard.setContent(content);
                    String filename = STR."\{appTitle}.txt";
                    try {
                        writeToFile(display.getText(), new File(filename));
                        getHostServices().showDocument(filename);
                    } catch (IOException ex) {
                        ex.fillInStackTrace();
                    }
                });
        sel.setOnAction(e -> {
            if (sel.getValue() != null) {
                println(sel.getValue()); // output to console
            }
        });
        openBtn.setOnAction(e -> {
            filer.setInitialDirectory(new File("."));
            filer.getExtensionFilters().add(extFilter);
            file = filer.showOpenDialog(stage);
            if (file != null) {
                status.setText(STR."\{file.getAbsolutePath()}  selected");
                readFileToDisplay(file);
            }
        });
        saveBtn.setOnAction(e -> {
            filer.setInitialDirectory(new File("."));
            filer.getExtensionFilters().add(extFilter);
            File file = filer.showSaveDialog(stage);
            if (file != null) {
                saveDisplayToFile(display.getText(), file);
            }
        });

        buttonBox.getChildren().addAll(runBtn, clearBtn, printBtn, openBtn, saveBtn);
        controls.getChildren().addAll(buttonBox, sel);

        root.setTop(form);
        root.setCenter(display);
        root.setBottom(status);
        return root;
    } // end createContent()

    /**
     * Initializes and displays the application's main stage. This method is
     * called when the application is launched. The setup method is invoked
     * to configure initial settings and components. If an exception occurs
     * during setup, it is caught and printed. Controls are added to the form,
     * and the stage is set up with a title, a new scene containing the
     * content created by {@code createContent()}, and finally displayed.
     *
     * @param stage The primary stage for this application, onto which the
     *              scene and other elements are set.
     */
    @Override
    public void start(Stage stage) {
        try {
            setup();
        } catch (Exception e) {
            println(e);
        }
        form.add(controls, 1, nextRow++);
        this.stage = stage;
        stage.setTitle(appTitle);
        Scene scene = new Scene(createContent());
        stage.setScene(scene);
        stage.show();
    }
    /**
     * An {@code ObservableList} that holds elements of type String, which are
     * used to populate the combo box in the application. This list is observable,
     * meaning it can be watched for changes, which helps in updating the UI
     * dynamically whenever items are added or removed. Replace "String" with
     * the object type that populates the ComboBox
     */
    private final ObservableList<String> obl = FXCollections.observableArrayList();
    /**
     * A {@code ComboBox} widget that displays a dropdown list of strings
     * to the user. It is backed by {@code obl}, the observable list of
     * strings, ensuring that any updates to {@code obl} are immediately
     * reflected in the choices available in the combo box.
     */
    private final ComboBox<String> sel = new ComboBox<>(obl);
    /**
     * Configures the initial settings for the application. This method sets
     * up the application's title, data entry form, form instructions, and
     * combo box items. It is designed to be called at the beginning of the
     * application's lifecycle to prepare the user interface components.
     *
     * The method sets the application's title to "Hello App". It creates
     * a form for data entry with fields for "Name" and "Age". If fields
     * are present, it sets instructions for form completion. Additionally,
     * it populates a combo box with example items and selects the first
     * item as default.
     *
     * @throws Exception If an error occurs during the setup process.
     */
    private void setup() throws Exception {
        appTitle = "Hello App";
        // Send comma-separated strings to the method below to generate data entry form:
        createForm("Name","Age");

        setFormInstructions(fields.length > 0 ? "Enter the following information" : "");

        // items 1, 2 and 3 are just examples of how to add to the combo box
        obl.addAll("item 1","item 2","item 3");
        sel.getSelectionModel().selectFirst();
    } // end setup
    /**
     * The instructions in this method are just samples.  Normally these lines
     * would be removed. This method Executes the primary functionality of the
     * application by retrieving user input and generating an output message.
     * This method fetches data entered by the user into predefined fields:
     * the first field (index 0) for the name, and the second field (index 1)
     * for the age. It parses the age input as an integer and constructs a
     * greeting message that includes the entered name, age, and the current
     * date. This message is then output to the application's display.
     * If any errors occur during data fetching or processing, such as a
     * number format exception when parsing the age, an exception is thrown.
     *
     * @throws Exception If there is an error in retrieving fields or
     *      parsing them, an exception is thrown to indicate
     *      such errors (e.g., {@link NumberFormatException} for age parsing).
     */
    private void run() throws Exception {
        String name = getField(0);
        int age = Integer.parseInt(getField(1));
        outputln(FMT."Hello \{name}. You are \{age} years old.  Todays date is \{LocalDate.now()}");
    } // end run

    // helper methods can go here
    private String getType(Object o) {
        return o.getClass().getSimpleName();
    }

    private void showJsonInBrowser(String json) {
        String jsonViewer = STR."https://codebeautify.org/jsonviewer?input=\{json}";
        println(jsonViewer);
        getHostServices().showDocument(jsonViewer);
    }
    /**
     * Displays a text input dialog with a specified prompt and collects the user's input.
     * This method initializes a {@code TextInputDialog}, sets its title to "Dialog", and uses the given prompt as the header text.
     * The dialog waits for the user to input text or close the dialog. The result is wrapped in an {@code Optional<String>}
     * to handle both user input and cases where no input is provided (e.g., the dialog is closed without input).
     *
     * @param prompt The header text displayed at the top of the dialog to prompt the user for input.
     * @return An {@code Optional<String>} containing the text entered by the user. If the user closes the dialog without
     *         entering text, the {@code Optional} is empty.
     */
    private Optional<String> getDialogText(String prompt) {
        var dialog = new TextInputDialog();
        dialog.setTitle("Dialog");
        dialog.setHeaderText(prompt);
        return dialog.showAndWait();
    }
    /**
     * Prompts the user for input using a dialog box and retrieves the entered text.
     * This method utilizes {@code getDialogText} to display a dialog box with the specified prompt.
     * If the user provides input and confirms, that input is returned. If the user cancels the dialog or closes it
     * without entering text, an empty string is returned as a default to handle cases where no input is provided.
     *
     * @param prompt The text displayed to the user in the dialog box, prompting them for input.
     * @return The text entered by the user, or an empty string if no input was provided.
     */
    private String input(String prompt) {
        var text = getDialogText(prompt);
        return text.orElse("");
    }
    /**
     * Prompts the user for an input and attempts to convert it into an integer.
     * This method displays a prompt to the user and reads the input as a string,
     * then attempts to convert this string into an integer. If the conversion
     * is successful, the integer value is returned. If the conversion fails
     * due to an input mismatch or any other issue, the method handles the
     * exception by returning a default value of -1.
     *
     * @param prompt The message displayed to the user prompting
     *               them to enter an integer.
     * @return The integer value input by the user, or -1 if
     *               the input is not a valid integer.
     */
    private int inputInt(String prompt) {
        try {
            return Integer.parseInt(input(prompt));
        } catch (Exception e) {
            return -1;
        }
    }
    /**
     * Prompts the user for an input and attempts to convert it into an double.
     * This method displays a prompt to the user and reads the input as a string,
     * then attempts to convert this string into an double. If the conversion is
     * successful, the integer value is returned. If the conversion fails due to
     * an input mismatch or any other issue, the method handles the exception by
     * returning a default value of -1.
     *
     * @param prompt The message displayed to the user
     *               prompting them to enter an double.
     * @return The integer value input by the user, or -1 if the
     *               input is not a valid double.
     */
    private double inputDouble(String prompt) {
        try {
            return Double.parseDouble(input(prompt));
        } catch (Exception e) {
            return -1;
        }
    }
    /**
     * Prompts the user for an input and attempts to convert it into an char.
     * This method displays a prompt to the user and reads the input as a string,
     * then attempts to convert this string into an char. If the conversion is
     * successful, the integer value is returned. If the conversion fails due
     * to an input mismatch or any other issue, the method handles the
     * exception by returning a default value of '.'.
     *
     * @param prompt The message displayed to the user prompting
     *               them to enter an char.
     * @return The integer value input by the user, or '.' if
     *               the input is not a valid char.
     */
    private char inputChar(String prompt) {
        try {
            return input(prompt).charAt(0);
        } catch (Exception e) {
            return '.';
        }
    }
    /**
     * Reads all lines from a specified file and returns them as an
     * array of strings. This method utilizes {@code readListFromFile}
     * to fetch the lines from the file into a list. It then converts
     * this list into an array of strings. If the file does not exist,
     * is empty, or an error occurs during reading, an empty array is
     * returned.
     *
     * @param fileName The name of the file from which to read the lines.
     * @return An array of strings, each representing
     *                     a line from the file. Returns an empty array
     *                     if the file is empty or cannot be read.
     */
    private String[] getLinesFromFile(String fileName) {
        var lines = readListFromFile(fileName);
        return lines.toArray(new String[0]);
    }
    /**
     * Reads the contents of a file specified by the file name and returns
     * them as a list of strings.
     *
     * @param fileName the name of the file to read
     * @return an ArrayList of strings containing the lines read from the file
     */
    private ArrayList<String> readListFromFile(String fileName) {
        var lines = new ArrayList<String>();
        try {
            lines = (ArrayList<String>)
                    Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            println(STR."\{lines.size()} lines read");
        } catch (IOException e) {
            showMessage(e.getMessage());
        }
        return lines;
    }
    /**
     * Reads the contents of the specified file and displays each line.
     * @param file the File object to read and display
     */
    private void readFileToDisplay(File file) {
        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputln(line);
            }
        } catch (IOException e) {
            showMessage(e.getMessage());
        }
    }
    /**
     * Reads the contents of the specified file
     * line by line and outputs each line.
     * @param file the File object to read
     */
    private void saveDisplayToFile(String content, File file) {
        try (var fileWriter = new FileWriter(file)) {
            fileWriter.write(content);
        } catch (IOException ex) {
            showMessage(ex.getMessage());
        }
    }
    /**
     * Writes the specified content to the given file.
     * @param content the content to write to the file
     * @param file the File object to write the content to
     * @throws IOException if an I/O error occurs while writing to the file
     */
    private void writeToFile(String content, File file) throws IOException {
        try (var reader = new BufferedReader(new StringReader(content))) {
            try (var writer = new PrintWriter(new FileWriter(file))) {
                reader.lines().forEach(writer::println);
            }
        }
    }
    /**
     * Inserts line breaks into a given string to ensure that no line exceeds
     * a specified maximum length. This method searches for space characters
     * to replace with newline characters ('\n'), aiming to break lines at
     * natural word boundaries. If a space character is found within the
     * maximum length limit, it is replaced by a newline. If no spaces are
     * found in a segment that exceeds the maximum length, the loop breaks
     * to prevent an infinite loop, and the text remains unchanged for that
     * segment.
     *
     * @param text The string into which line breaks are to be inserted.
     * @param max The maximum allowed length of any line of text
     *            before a line break is inserted.
     * @return The modified string with line breaks inserted to
     *         prevent any line from exceeding the specified maximum length.
     */
    private String insertLineBreaks(String text, int max) {
        StringBuilder sb = new StringBuilder(text);
        int lastSpaceIndex = 0;
        int nextSearchStart = 0;

        while (nextSearchStart + max < sb.length()) {
            int spaceIndex = sb.lastIndexOf(" ", nextSearchStart + max);
            if (spaceIndex > lastSpaceIndex) {
                sb.replace(spaceIndex, spaceIndex + 1, "\n");
                lastSpaceIndex = spaceIndex;
                nextSearchStart = spaceIndex + 1;
            } else break; // No spaces found, exit to avoid an infinite loop
        }
        return sb.toString();
    }

    private void showMessage(String message) {
        var alert = new Alert(AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    private void output(Object value) {
        var stringValue = String.valueOf(value);
        if (stringValue.isEmpty())
            return;
        output.append(stringValue);
        updateOutput();
    }
    /**
     * Appends the given value to the application's output buffer, followed
     * by a newline, and updates the display. This method converts the
     * provided value to a string using {@code String.valueOf}. If the
     * resulting string is not empty, it appends this string and a newline
     * character to the output buffer. After appending the text,
     * it calls {@code updateOutput} to refresh the display, ensuring
     * that the new output is visible to the user. If the string value
     * of the provided object is empty, the method does nothing and
     * returns immediately.
     *
     * @param value The object to be output, which can be of any type.
     *              The object is converted to a string representation.
     */
    private void outputln(Object value) {
        var stringValue = String.valueOf(value);
        if (stringValue.isEmpty())
            return;
        output.append(stringValue).append("\n");
        updateOutput();
    }

    private void outputln() {
        output.append("\n");
        updateOutput();
    }
    /**
     * Updates the graphical user interface to reflect changes in the
     * application's output. This method ensures that updates to the
     * display text are executed on the JavaFX Application Thread, which
     * is necessary for thread safety in a JavaFX environment. It sets the
     * text of a predefined display component (e.g., a TextArea) to the
     * current value stored in an {@code output} object.
     *
     * The method uses {@code Platform.runLater} to queue the update,
     * ensuring that it is executed in the correct thread context.
     * If an exception occurs during the update, it is caught and
     * printed using the {@code println} method.
     */
    private void updateOutput() {
        Platform.runLater(() -> {// force updates to be on the FX app thread
            try {
                display.setText(output.toString());
            } catch (Exception ex) {
                println(ex);
            }
        });
    }

    private void clearOutput() {
        output.setLength(0);
        display.setText(output.toString());
    }

    private void clear() {
        for (var field : fields)
            field.setText("");
    }
    private boolean isValidIndex(int index) {
        return index >= 0 && index < rows;
    }

    private void clearField(int index) {
        if (isValidIndex(index))
            fields[index].setText("");
    }

    private TextField getTextField(int index) {

        return isValidIndex(index) ? fields[index] : null;
    }

    private String getField(int index) {

        return isValidIndex(index) ? fields[index].getText() : "";
    }

    private void setField(int index, String value) {
        if (isValidIndex(index))
            fields[index].setText(value);
    }

    private String getLabel(int index) {

        return isValidIndex(index) ? labels[index].getText() : "";
    }

    private void setLabel(int index, String value) {
        if (isValidIndex(index))
            labels[index].setText(value);
    }
    private void setFormInstructions(String value) {
        instructions.setText(value);
    }

    /**
     * This method creates a text field entry for every prompt in prompts
     * and displays a data entry form at the top of the UI for the purpose
     * of getting information from the user at runtime.  All the text field
     * objects are part of an array called fields. The companion method
     * getField(0) returns the String value that the user enters in the first
     * field.
     * @param prompts (prompts can be empty and in such case no form is generated.
     */
    private void createForm(String... prompts) {
        form.add(instructions, 1, 0);
        form.setAlignment(Pos.CENTER_LEFT);
        var column1 = new ColumnConstraints();
        column1.setHalignment(HPos.RIGHT);
        form.getColumnConstraints().add(column1);
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(25, 25, 25, 25));
        rows = prompts.length;
        nextRow = rows + 1;
        fields = new TextField[rows];
        labels = new Label[rows];

        for (int i = 1; i <= rows; i++) {
            String label = prompts[i - 1] + ":";
            fields[i - 1] = new TextField();
            if (label.startsWith("p-")) {
                label = label.substring(2);
                fields[i - 1] = new PasswordField();
            }
            labels[i - 1] = new Label(label);
            fields[i - 1].setMaxWidth(720);
            fields[i - 1].setPrefWidth(400);
            form.add(labels[i - 1], 0, i);
            form.add(fields[i - 1], 1, i);
        }
    } // end createForm

    private int getRandom(int max) {
        return random.nextInt(max);
    }

    private int getRandom(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private void println() {
        System.out.println();
    }

    private void print(Object value) {
        System.out.print(value);
    }

    private void println(Object value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        launch(args);
    } // end main
} // end class

