import os
import tkinter as tk
from tkinter import ttk
from zhihu import *

class ArticleViewer(tk.Toplevel):
    def __init__(self, master=None, **kwargs):
        super().__init__(master, **kwargs)
        self.title("查看回答")
        self.text = tk.Text(self, wrap="word", width=150, height=50)
        self.text.pack(padx=10, pady=10)
        self.create_widgets()
        self.current_index = 1
        self.max_comments=50
    def create_widgets(self):
        self.previous_button = tk.Button(self, text="前一篇", command=self.show_previous_article)
        self.previous_button.pack(side="left", padx=10, pady=10)

        self.next_button = tk.Button(self, text="后一篇", command=self.show_next_article)
        self.next_button.pack(side="right", padx=10, pady=10)

    def show_next_article(self):
        self.current_index += 1
        if 0 <= self.current_index <=  self.max_comments:
            self.show_current_article()

    def show_previous_article(self):
        self.current_index -= 1
        if 0 <= self.current_index <=  self.max_comments:
            self.show_current_article()

    def show_current_article(self):
        filename = f"spider/articles/article_{ self.current_index}.md"

        if os.path.exists(filename):
            with open(filename, "r", encoding="utf-8") as file:
                content = file.read()
                self.text.delete("1.0", tk.END)
                self.text.insert("1.0", content)

class ZhihuApp:
    def __init__(self, master):
        self.master = master
        master.title("知乎热榜")
        self.create_widgets()
        self.current_index = 0
        self.article_viewer = None

    def create_widgets(self):
        self.label = tk.Label(self.master, text="知乎热榜", font=("Helvetica", 16))
        self.label.grid(row=0, column=0, columnspan=3, pady=20)

        self.tree = ttk.Treeview(self.master, columns=("ID", "题目"), show="headings", height=20)
        
        self.tree.heading("ID", text="ID")
        self.tree.heading("题目", text="Title")
        self.tree.column("ID", width=50)
        self.tree.column("题目", width=700)
        self.tree.grid(row=1, column=0, columnspan=3, pady=10)
        y_scrollbar = ttk.Scrollbar(self.master, orient="vertical", command=self.tree.yview)
        y_scrollbar.grid(row=1, column=3, sticky="ns")
        self.tree.configure(yscrollcommand=y_scrollbar.set)

        # 添加滑动条
        x_scrollbar = ttk.Scrollbar(self.master, orient="horizontal", command=self.tree.xview)
        x_scrollbar.grid(row=3, column=0, columnspan=3, sticky="ew")
        self.tree.configure(xscrollcommand=x_scrollbar.set)
        self.load_articles_button = tk.Button(self.master, text="热榜获取", command=self.load_articles)
        self.load_articles_button.grid(row=2, column=0, pady=10)

        self.view_article_button = tk.Button(self.master, text="获取回答", command=self.collect_article)
        self.view_article_button.grid(row=2, column=1,pady=10)

        self.view_article_button = tk.Button(self.master, text="查看回答", command=self.view_article)
        self.view_article_button.grid(row=2, column=3,pady=10)

    def load_articles(self):
        create_database()
        self.tree.delete(*self.tree.get_children())
        self.articles = zhihu_hot()
        for article in self.articles:
            self.tree.insert("", "end", values=article)

    def collect_article(self):
        selected_item = self.tree.selection()
        values = self.tree.item(selected_item, "values")

        # 假设第一个id为values[0]
        self.article_id = values[0]
        get_answer(values[0])

    def view_article(self):
        filename = f"spider/articles/article_1.md"

        if os.path.exists(filename):
            if self.article_viewer:
                self.article_viewer.destroy()

            self.article_viewer = ArticleViewer(self.master)
            with open(filename, "r", encoding="utf-8") as file:
                content = file.read()
                self.article_viewer.text.insert("1.0", content)
        else:
            print(f"Markdown file not found for article_1")

if __name__ == "__main__":
    root = tk.Tk()
    app = ZhihuApp(root)
    root.mainloop()