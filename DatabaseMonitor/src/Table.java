import java.util.ArrayList;
import java.util.List;

public class Table {
    List<String> data;
    int nColumns;

    public List<String> getData() {
        return data;
    }

    public int getnColumns() {
        return nColumns;
    }

    public Table(List<String> data, int nColumns){
        this.data = data;
        this.nColumns = nColumns;

    }

    public void printTable() {
        List<Integer> colWidths = calcWidth();
        int nRows = data.size() / nColumns;


        printBorder(colWidths);

        printRow(0, colWidths);
        printBorder(colWidths);

        for (int i = 1; i < nRows; i++) {
            printRow(i, colWidths);
        }

        printBorder(colWidths);
    }

    private void printBorder(List<Integer> columnWidths) {
        for (int width : columnWidths) {
            System.out.print("+");
            for (int i = 0; i < width + 2; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }

    private void printRow(int row, List<Integer> columnWidths) {
        int firstElement = row * nColumns;
        for (int i = firstElement; i < firstElement+nColumns ; i++) {
            System.out.print("| " + data.get(i));
            for (int j = 0; j < columnWidths.get(i%nColumns) - data.get(i).length() + 1; j++) {
                System.out.print(" ");
            }
        }
        System.out.println("|");
    }

    private List<Integer> calcWidth(){
        List<Integer> colWidths = new ArrayList<Integer>();
        for (int i = 0; i < nColumns; i++) {
            colWidths.add(0);
        }
        int i = 0;
        for(String str : data){
            int length = str.length();
            if(length > colWidths.get(i)){
                colWidths.set(i, length);
            }
            if(i + 1 >= nColumns){
                i = 0;
            } else {
                i++;
            }
        }
        return colWidths;
    }
}
