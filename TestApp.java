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
import java.util.stream.IntStream;

import static java.util.FormatProcessor.FMT;

public class TestApp extends Application {

    private final BorderPane root = new BorderPane();

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 600;
    private static final int OUTPUT_FONT_SIZE = 18;
    private static final int DEFAULT_PADDING = 10;
    private static final String STYLE = STR."""
                -fx-font-family: monospace;
                -fx-font-weight: bold;
                -fx-font-size: \{OUTPUT_FONT_SIZE};
                """;
    private final HBox buttonBox = new HBox(DEFAULT_PADDING);
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

    private final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance();

    private String appTitle;
    private Label[] labels;
    private TextField[] fields;
    private int rows;
    private int nextRow;

    private Parent createContent() {
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setPadding(new Insets(DEFAULT_PADDING));
        root.setStyle(STYLE);

        display.setPrefColumnCount(50);
        display.setWrapText(true);
        display.setEditable(false);
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
            if (sel.getValue().isBlank())
                return;
            outputln(sel.getValue()); // action when combo box item is selected
        });

        buttonBox.getChildren().addAll(runBtn, clearBtn, printBtn, sel);

        root.setTop(form);
        root.setCenter(display);
        root.setBottom(status);
        return root;
    } // end createContent()

    @Override
    public void start(Stage stage) {
        try {
            setup();
        } catch (Exception e) {
            println(e);
        }
        form.add(buttonBox, 1, nextRow++);
        stage.setTitle(appTitle);
        Scene scene = new Scene(createContent());
        stage.setScene(scene);
        stage.show();
    }

    // Replace "String" below with the object type that populates the ComboBox
    private final ObservableList<String> obl = FXCollections.observableArrayList();
    private final ComboBox<String> sel = new ComboBox<>(obl);

    private void setup() throws Exception {
        appTitle = "Hello App";
        // Send comma-separated strings to the method below to generate data entry form:
        createForm("Name", "Age");

        setFormInstructions(fields.length > 0 ? "Enter the following information" : "");

        // items 1, 2 and 3 are just examples of how to add to the combo box
        obl.addAll("item 1", "item 2", "item 3");
        sel.getSelectionModel().selectFirst();
    } // end setup

    private void run() throws Exception {
        String name = getField(0);
        int age = Integer.parseInt(getField(1));
        outputln(FMT."Hello \{name}. You are \{age} years old");
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

    private Optional<String> getDialogText(String prompt) {
        var dialog = new TextInputDialog();
        dialog.setTitle("Dialog");
        dialog.setHeaderText(prompt);
        return dialog.showAndWait();
    }

    private String input(String prompt) {
        var text = getDialogText(prompt);
        return text.orElse("");
    }

    private int inputInt(String prompt) {
        try {
            return Integer.parseInt(input(prompt));
        } catch (Exception e) {
            return -1;
        }
    }

    private double inputDouble(String prompt) {
        try {
            return Double.parseDouble(input(prompt));
        } catch (Exception e) {
            return -1;
        }
    }

    private char inputChar(String prompt) {
        try {
            return input(prompt).charAt(0);
        } catch (Exception e) {
            return '.';
        }
    }

    private String[] getLinesFromFile(String fileName) {
        var lines = readListFromFile(fileName);
        return lines.toArray(new String[0]);
    }

    private ArrayList<String> readListFromFile(String fileName) {
        var lines = new ArrayList<String>();
        try {
            lines = (ArrayList<String>) Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            println(STR."\{lines.size()} lines read");
        } catch (IOException e) {
            showMessage(e.getMessage());
        }
        return lines;
    }

    private void writeToFile(String string, File file) throws IOException {
        try (var reader = new BufferedReader(new StringReader(string))) {
            try (var writer = new PrintWriter(new FileWriter(file))) {
                reader.lines().forEach(writer::println);
            }
        }
    }

    private String insertLineBreaks(String text, int max) {
        var sb = new StringBuilder(text);
        int i = 0;
        while ((i = sb.indexOf(" ", i + max)) != -1)
            sb.replace(i, i + 1, "\n");
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

    private void updateOutput() {
        Platform.runLater(() -> {// force updates to be run on the FX application thread
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
     *
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

