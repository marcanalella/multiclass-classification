package com.example.demo;

import org.springframework.stereotype.Service;
import weka.attributeSelection.*;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class Application {

    String multiClassesFile;
    BufferedWriter writer;
    String PATH;
    String ARFF = ".arff";
    String FINAL = "_final";
    String TXT = ".txt";

    public void run(String path) throws Exception {
        PATH = path;
        List<String> fileNames = init();
        List<String> datasets = stepOne(fileNames);
        stepTwo(datasets);
        stepThree();
        stepFour(datasets);
        stepFive(datasets);
    }

    private List<String> init() throws Exception {
        File folder = new File(PATH);
        File[] fList = folder.listFiles();
        if (fList != null) {
            for (File value : fList) {
                String pes = value.getName();
                if (pes.endsWith(ARFF) || pes.endsWith(TXT)) {
                    try {
                        boolean success = (new File(value.toString()).delete());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }

        List<String> fileNames = new ArrayList<>();
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(PATH));
            directoryStream.forEach(path -> fileNames.add(path.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }

        if (fileNames.stream().filter(file -> file.contains("csv")).count() < 2) {
            throw new Exception();
        }

        return fileNames.stream().filter(file -> file.contains(".csv")).collect(Collectors.toList());
    }

    private List<String> stepOne(List<String> fileNames) throws Exception {
        List<String> fileNamesArff = new ArrayList<>();
        List<Instances> instances = new ArrayList<>();
        AtomicInteger index = new AtomicInteger();
        fileNames.forEach(
                file -> {
                    CSVLoader loader = new CSVLoader();
                    ArffSaver saver = new ArffSaver();
                    try {
                        index.getAndIncrement();
                        loader.setSource(new File(file));
                        Instances data = loader.getDataSet();
                        //REMOVE
                        data = getInstances(data);
                        //ATTRIBUTO
                        FastVector values = new FastVector();
                        values.addElement("C" + index);
                        values.addElement("not_C" + index );
                        data.insertAttributeAt(new Attribute("Class", values), data.numAttributes());
                        for (int i = 0; i < data.numInstances(); i++) {
                            data.instance(i).setValue(data.numAttributes() - 1, "C" + index);
                        }
                        data.setRelationName("C" + index);
                        saver.setInstances(data);
                        String name = (file + ARFF).replace(".csv", "");
                        saver.setFile(new File(name));
                        saver.writeBatch();
                        instances.add(data);
                        fileNamesArff.add(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (Exception ex) {}
                    }
                }
        );

        //CREO IL DATASET METTENDO ASSIEME TUTTE LE ISTANZE PRESENTI NEI CSV
        int asize = instances.get(0).numAttributes();
        boolean[] strings_pos = new boolean[asize];
        for (int i = 0; i < asize; i++) {
            Attribute att = instances.get(0).attribute(i);
            strings_pos[i] = ((att.type() == Attribute.STRING) ||
                    (att.type() == Attribute.NOMINAL));
        }
        Instances multiClasses = new Instances(instances.get(0));
        AtomicReference<String> relationName = new AtomicReference<>("");
        //ATTRIBUTO
        FastVector values = new FastVector();
        instances.forEach(instance -> {
            relationName.set(relationName + instance.relationName() + "_");
            values.addElement(instance.relationName());
        });
        multiClasses = getInstances(multiClasses);
        multiClasses.insertAttributeAt(new Attribute("Class", values), multiClasses.numAttributes());
        for (int i = 0; i < multiClasses.numInstances(); i++) {
            multiClasses.instance(i).setValue(multiClasses.numAttributes() - 1, "C1");
        }
        Instances finalMultiClasses = multiClasses;
        instances.stream().skip(1).forEach(
                istance -> {
                    DataSource source = new DataSource(istance);
                    Instances instancs = null;
                    try {
                        instancs = source.getStructure();
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (Exception ex) {}
                    }
                    Instance instance;
                    while (source.hasMoreElements(instancs)) {
                        instance = source.nextElement(instancs);
                        finalMultiClasses.add(instance);

                        // COPIA GLI ATTRIBUTI STRING
                        for (int j = 0; j < asize; j++) {
                            if (strings_pos[j]) {
                                finalMultiClasses.instance(finalMultiClasses.numInstances() - 1)
                                        .setValue(j, instance.stringValue(j));
                            }
                        }
                    }
                });
        finalMultiClasses.setRelationName(relationName.toString().substring(0, relationName.toString().length() - 1));
        ArffSaver saver = new ArffSaver();
        try {
            saver.setInstances(finalMultiClasses);
            saver.setFile(new File(PATH + "\\" + "multiClasses" + ARFF));
            saver.writeBatch();
            multiClassesFile = PATH + "\\" + "multiClasses" + ARFF;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        //CREO LE CLASSI I-ESIME DOVE LA CLASSE POSITIVA RAPPRESENTANO TUTTE LE ISTANZE DELLA CLASSE I-ESIMA E
        //LE ISTANZE DELLA CLASSE NEGATIVA SONO TUTTE QUELLE DELLE CLASSI RESTANTI
        index.set(0);
        List<String> finalFileNamesArff = new ArrayList<>();
        fileNamesArff.forEach(
                fileArff -> {
                    Instances data;
                    try {
                        data = new Instances(new BufferedReader(new FileReader(fileArff)));
                        index.getAndIncrement();
                        instances.stream().filter(e-> !e.relationName().equals(data.relationName())).forEach(
                                istance -> {
                                    DataSource source = new DataSource(istance);
                                    Instances instants = null;
                                    try {
                                        instants = source.getStructure();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        try {
                                            throw e;
                                        } catch (Exception ignored) {
                                        }
                                    }
                                    Instance instance;
                                    while (source.hasMoreElements(instants)) {
                                        instance = source.nextElement(instants);
                                        data.add(instance);

                                        // COPIA GLI ATTRIBUTI STRING
                                        for (int j = 0; j < asize; j++) {
                                            if (strings_pos[j]) {
                                                data.instance(data.numInstances() - 1)
                                                        .setValue(j, "not_C" + index);
                                            }
                                        }
                                    }
                                });
                        saver.setInstances(data);
                        saver.setFile(new File(fileArff + FINAL + ARFF));
                        saver.writeBatch();
                        finalFileNamesArff.add(fileArff + FINAL + ARFF);
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (IOException ignored) {}
                    }
                }
        );
        return finalFileNamesArff;
    }

    private void stepTwo(List<String> fileNamesArff) {
        J48 j48 = new J48();
        fileNamesArff.forEach(
                fileArff -> {
                    try {
                        J48Filter(j48, fileArff);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (Exception ignored) {}
                    }
                });
    }

    private void stepThree() throws Exception {
        J48 j48 = new J48();
        J48Filter(j48, multiClassesFile);
    }

    private void stepFour(List<String> fileNamesArff) {
        J48 j48 = new J48();
        fileNamesArff.forEach(
                fileArff -> {
                    try {
                        J48FilterWithSMOTE(j48, fileArff);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (Exception ignored) {}
                    }
                });
    }

    private void stepFive(List<String> fileNamesArff) {
        fileNamesArff.forEach(
                fileArff -> {
                    try {
                        metaClassifiersCfsSubsetEvalBestFirst(fileArff);
                        metaClassifiersInfogainRanker(fileArff);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            throw e;
                        } catch (Exception ignored) {}
                    }
                }
        );
    }


    private Instances getInstances(Instances multiClasses) throws Exception {
        String[] options = new String[2];
        options[0] = "-R";
        options[1] = Integer.toString(multiClasses.numAttributes());
        Remove remove = new Remove();
        remove.setOptions(options);
        remove.setInputFormat(multiClasses);
        multiClasses = Filter.useFilter(multiClasses, remove);
        return multiClasses;
    }

    private void J48Filter(J48 j48, String multiClassesFile) throws Exception {
        try {
            Instances data = prepareData(multiClassesFile);
            evaluation(j48, data, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void J48FilterWithSMOTE(J48 j48, String multiClassesFile) throws Exception {
        try {
            Instances data = prepareData(multiClassesFile);
            SMOTE smote = new SMOTE();
            smote.setInputFormat(data);
            Instances dataWithSmote = Filter.useFilter(data, smote);
            evaluation(j48, dataWithSmote, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void evaluation(J48 j48, Instances instance, Boolean withSmote) throws Exception {
        Evaluation evaluation = new Evaluation(instance);
        ArffSaver saver = new ArffSaver();
        saver.setInstances(instance);
        evaluation.crossValidateModel(j48, instance, 5, new Random(1));
        String result = evaluation.toSummaryString("Results J48 for " + instance.relationName() + "\n\n", true);
        if (withSmote) {
            saver.setFile(new File("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultWithSMOTE" + instance.relationName() + ARFF));
            writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultWithSMOTE" + instance.relationName() + ".txt"));
        } else {
            saver.setFile(new File("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\result" + instance.relationName() + ARFF));
            writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\result" + instance.relationName() + ".txt"));
        }
        writer.write(result);
        writer.close();
        saver.writeBatch();
    }

    private Instances prepareData(String instanceFile) throws Exception {
        Instances data = new Instances(new BufferedReader(new FileReader(instanceFile)));
        DataSource dataSource = new DataSource(data);
        Instances instance = dataSource.getDataSet();
        if (instance.classIndex() == -1) {
            System.out.println("reset index...");
            instance.setClassIndex(data.numAttributes() - 1);
        }
        return data;
    }

    private void metaClassifiersCfsSubsetEvalBestFirst(String classFile) throws Exception {
        // CfsSubsetEval + best first
        Instances data = prepareData(classFile);
        //APPLICO SMOTE
        SMOTE smote = new SMOTE();
        smote.setInputFormat(data);
        data = Filter.useFilter(data, smote);
        //AGGIUNGO ATTRIBUTE SELECTION DEL TIPO cfsSubsetEval + best first
        AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
        CfsSubsetEval eval = new CfsSubsetEval();
        BestFirst search = new BestFirst();
        NaiveBayes nb = new NaiveBayes();
        classifier.setClassifier(nb);
        classifier.setEvaluator(eval);
        classifier.setSearch(search);
        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(classifier, data, 5, new Random(1));
        String result = evaluation.toSummaryString("Results CfsSubsetEval+BestFirst for " + data.relationName() + "\n\n", true);
        writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultCfsSubsetEval+BestFirst" + data.relationName() + ".txt"));
        writer.write(result);
        writer.close();
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultCfsSubsetEval+BestFirst" + data.relationName() + ARFF));
        saver.writeBatch();
        getAttributeSelectorCfsSubsetEvalBestFirst(eval, search, data);
    }

    private void metaClassifiersInfogainRanker(String classFile) throws Exception {
        // CfsSubsetEval + best first
        Instances data = prepareData(classFile);
        //APPLICO SMOTE
        SMOTE smote = new SMOTE();
        smote.setInputFormat(data);
        data = Filter.useFilter(data, smote);
        //AGGIUNGO ATTRIBUTE SELECTION DEL TIPO cfsSubsetEval + best first
        AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
        InfoGainAttributeEval eval = new InfoGainAttributeEval();
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(Math.min(1, data.numAttributes() - 1));
        NaiveBayes nb = new NaiveBayes();
        classifier.setClassifier(nb);
        classifier.setEvaluator(eval);
        classifier.setSearch(ranker);
        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(classifier, data, 5, new Random(1));
        String result = evaluation.toSummaryString("Results Infogain + Ranker for " + data.relationName() + "\n\n", true);
        writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultInfogain+Ranker" + data.relationName() + ".txt"));
        writer.write(result);
        writer.close();
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\resultInfogain+Ranker" + data.relationName() + ARFF));
        saver.writeBatch();
        getAttributeSelectorInfogainRanker(eval, ranker, data);
    }

    private void getAttributeSelectorCfsSubsetEvalBestFirst(CfsSubsetEval evaluator, BestFirst search, Instances data) throws Exception {
        AttributeSelection selector = new AttributeSelection();
        selector.setEvaluator(evaluator);
        selector.setSearch(search);
        selector.SelectAttributes(data);
        writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\selettoriCfsSubsetEval" + data.relationName() + ".txt"));
        writer.write("Selettori CfsSubsetEval + BestFirst data " + data.relationName() + "\n\n");
        writer.write(Arrays.toString(selector.selectedAttributes()));
        writer.close();
    }
    private void getAttributeSelectorInfogainRanker(InfoGainAttributeEval evaluator, Ranker ranker, Instances data) throws Exception {
        AttributeSelection selector = new AttributeSelection();
        selector.setEvaluator(evaluator);
        selector.setSearch(ranker);
        selector.SelectAttributes(data);
        writer = new BufferedWriter(new FileWriter("C:\\Users\\065863758\\Desktop\\demo\\demo\\src\\main\\resources\\irisSingleCl\\selettoriInfogainRanker" + data.relationName() + ".txt"));
        writer.write("Selettori Infogain + Ranker data " + data.relationName() + "\n\n");
        writer.write(Arrays.toString(selector.selectedAttributes()));
        writer.close();
    }
}