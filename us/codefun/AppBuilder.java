package us.codefun;

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

import java.security.*;
import java.time.*;
import java.text.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class AppBuilder extends Application {

    private final BorderPane root = new BorderPane();

    private final Button runBtn = new Button("Run");
    private final Button clearBtn = new Button("Clear Output");
    private final Button printBtn = new Button("Print");
    private final TextArea display = new TextArea();
    private final Label status = new Label("Status");
    private final StringBuilder output = new StringBuilder(128);
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final ClipboardContent content = new ClipboardContent();
    private final GridPane form = new GridPane();
    private final Canvas canvas = new Canvas(600, 600);
    private final SecureRandom random = new SecureRandom();
    private final Label instructions = new Label();

    // form variables declared
    private String appTitle;
    private Label[] labels;
    private TextField[] fields;
    private int rows;
    private int nextRow;

    private int outputCount;

    private final HBox buttonBox = new HBox(10);

    private Parent createContent() {
        root.setPrefSize(800, 600);
        root.setPadding(new Insets(10));

        display.setPrefColumnCount(50);
        display.setWrapText(true);
        display.setEditable(false);
        display.setStyle("""
                -fx-font-family: monospace;
                -fx-font-weight: bold;
                -fx-font-size: 18;
                """);
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
                    String filename = appTitle + ".txt";
                    try {
                        writeToFile(display.getText(), new File(filename));
                        getHostServices().showDocument(filename);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
        sel.setOnAction(e -> {
            if (sel.getValue().isBlank())
                return;
            root.setStyle("-fx-background-color:%s".formatted(sel.getValue()));
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
        appTitle = "App";
        setFormInstructions("");

        // Send comma-separated strings to the method below to generate data entry form:
        setFormPrompts("Enter your name");

        // see complete list of javafx named colors
        // at https://openjfx.io/javadoc/18/javafx.graphics/javafx/scene/doc-files/cssref.html
        obl.addAll("whitesmoke","lightgray","azure","beige","green","cyan");
    } // end setup

    public void run() throws Exception {
        String name = getField(0);
        outputln(name);
    } // end run

    // helper methods can go here
    private String getType(Object o) {
        return o.getClass().getSimpleName();
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
        Scanner reader = null;
        try {
            reader = new Scanner(new File(fileName));
            while (reader.hasNextLine()) lines.add(reader.nextLine());
        } catch (IOException e) {
            showMessage(e.getMessage());
        }
        if (reader != null) reader.close();
        print("Lines read: " + lines.size());
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
        if (stringValue.equals(""))
            return;
        output.append(stringValue);
        updateOutput();
    }

    private void outputln(Object value) {
        var stringValue = String.valueOf(value);
        if (stringValue.equals(""))
            return;
        output.append(stringValue).append("\n");
        updateOutput();
    }

    private void outputln() {
        output.append("\n");
        updateOutput();
    }

    private void updateOutput() {
        display.setText(output.toString());
        outputCount++;
    }

    private void clearOutput() {
        output.setLength(0);
        outputCount = 0;
        display.setText(output.toString());
    }

    private void clear() {
        for (var field : fields)
            field.setText("");
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

    private void setLabel(int index, String value) {
        if (isValidIndex(index))
            labels[index].setText(value);
    }

    private void setFormInstructions(String value) {
        instructions.setText(value);
    }

    private void setFormPrompts(String... prompts) {
        createForm(prompts);
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < rows;
    }

    private void createForm(String... prompts) {
        createForm(Pos.CENTER_LEFT, HPos.RIGHT, prompts);
    }

    private void createForm(Pos alignment, HPos labelAlign, String[] prompts) {
        form.add(instructions, 1, 0);
        form.setAlignment(alignment);
        var column1 = new ColumnConstraints();
        column1.setHalignment(labelAlign);
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

